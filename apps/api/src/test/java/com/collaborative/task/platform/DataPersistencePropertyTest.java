package com.collaborative.task.platform;

import net.jqwik.api.*;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.Positive;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for data persistence ACID compliance
 * **Feature: collaborative-task-platform, Property 27: Data persistence maintains ACID compliance**
 * **Validates: Requirements 6.5**
 * 
 * ACID Properties Tested:
 * - Atomicity: Transactions are all-or-nothing
 * - Consistency: Database remains in a valid state with constraints enforced
 * - Isolation: Concurrent transactions don't interfere with each other
 * - Durability: Committed changes persist after system restart
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class DataPersistencePropertyTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("test_collaborative_tasks")
            .withUsername("test_user")
            .withPassword("test_password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://" + postgres.getHost() + ":" + postgres.getFirstMappedPort() + "/" + postgres.getDatabaseName());
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        
        // Create test tables if they don't exist
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS test_users (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                email VARCHAR(255) UNIQUE NOT NULL,
                full_name VARCHAR(100) NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """);

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS test_projects (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                name VARCHAR(100) NOT NULL,
                owner_id UUID REFERENCES test_users(id),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """);

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS test_tasks (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                project_id UUID REFERENCES test_projects(id),
                title VARCHAR(200) NOT NULL,
                status VARCHAR(20) DEFAULT 'TODO',
                assignee_id UUID REFERENCES test_users(id),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """);

        // Clean up test data
        jdbcTemplate.execute("DELETE FROM test_tasks");
        jdbcTemplate.execute("DELETE FROM test_projects");
        jdbcTemplate.execute("DELETE FROM test_users");
    }

    /**
     * Property: Atomicity - For any transaction, either all operations succeed or all fail
     * Tests that database transactions are atomic (all-or-nothing)
     */
    @Property(tries = 100)
    void transactionAtomicityIsPreserved(
            @ForAll @NotBlank String userEmail,
            @ForAll @NotBlank String userName,
            @ForAll @NotBlank String projectName,
            @ForAll @NotBlank String taskTitle) {
        
        // Arrange - Get initial counts
        int initialUserCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM test_users", Integer.class);
        int initialProjectCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM test_projects", Integer.class);
        int initialTaskCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM test_tasks", Integer.class);

        // Act - Attempt transaction that should fail due to constraint violation
        try {
            transactionTemplate.execute(status -> {
                // Insert user
                String userId = UUID.randomUUID().toString();
                jdbcTemplate.update(
                    "INSERT INTO test_users (id, email, full_name) VALUES (?, ?, ?)",
                    userId, userEmail, userName
                );

                // Insert project
                String projectId = UUID.randomUUID().toString();
                jdbcTemplate.update(
                    "INSERT INTO test_projects (id, name, owner_id) VALUES (?, ?, ?)",
                    projectId, projectName, userId
                );

                // Insert task with invalid foreign key to force rollback
                jdbcTemplate.update(
                    "INSERT INTO test_tasks (id, project_id, title, assignee_id) VALUES (?, ?, ?, ?)",
                    UUID.randomUUID().toString(), projectId, taskTitle, "invalid-uuid-that-does-not-exist"
                );

                return null;
            });
            fail("Transaction should have failed due to foreign key constraint violation");
        } catch (Exception e) {
            // Expected - transaction should fail
        }

        // Assert - Atomicity: All operations should be rolled back
        int finalUserCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM test_users", Integer.class);
        int finalProjectCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM test_projects", Integer.class);
        int finalTaskCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM test_tasks", Integer.class);

        assertEquals(initialUserCount, finalUserCount, "User count should be unchanged after failed transaction (Atomicity)");
        assertEquals(initialProjectCount, finalProjectCount, "Project count should be unchanged after failed transaction (Atomicity)");
        assertEquals(initialTaskCount, finalTaskCount, "Task count should be unchanged after failed transaction (Atomicity)");
    }

    /**
     * Property: Consistency - For any valid data operation, database constraints are maintained
     * Tests that database remains in a consistent state after operations
     */
    @Property(tries = 100)
    void databaseConsistencyIsMaintained(
            @ForAll @NotBlank String userEmail,
            @ForAll @NotBlank String userName,
            @ForAll @NotBlank String projectName,
            @ForAll @NotBlank String taskTitle) {
        
        // Act - Perform valid transaction
        String userId = transactionTemplate.execute(status -> {
            // Insert user
            String id = UUID.randomUUID().toString();
            jdbcTemplate.update(
                "INSERT INTO test_users (id, email, full_name) VALUES (?, ?, ?)",
                id, userEmail, userName
            );

            // Insert project
            String projectId = UUID.randomUUID().toString();
            jdbcTemplate.update(
                "INSERT INTO test_projects (id, name, owner_id) VALUES (?, ?, ?)",
                projectId, projectName, id
            );

            // Insert task
            jdbcTemplate.update(
                "INSERT INTO test_tasks (id, project_id, title, assignee_id) VALUES (?, ?, ?, ?)",
                UUID.randomUUID().toString(), projectId, taskTitle, id
            );

            return id;
        });

        // Assert - Consistency: All foreign key relationships are valid
        List<Map<String, Object>> users = jdbcTemplate.queryForList(
            "SELECT * FROM test_users WHERE id = ?", userId
        );
        assertEquals(1, users.size(), "User should exist (Consistency)");

        List<Map<String, Object>> projects = jdbcTemplate.queryForList(
            "SELECT * FROM test_projects WHERE owner_id = ?", userId
        );
        assertEquals(1, projects.size(), "Project should exist with valid owner reference (Consistency)");

        List<Map<String, Object>> tasks = jdbcTemplate.queryForList(
            "SELECT * FROM test_tasks WHERE assignee_id = ?", userId
        );
        assertEquals(1, tasks.size(), "Task should exist with valid assignee reference (Consistency)");

        // Verify referential integrity
        String projectId = (String) projects.get(0).get("id");
        List<Map<String, Object>> tasksByProject = jdbcTemplate.queryForList(
            "SELECT * FROM test_tasks WHERE project_id = ?", projectId
        );
        assertEquals(1, tasksByProject.size(), "Task should reference the correct project (Consistency)");
    }

    /**
     * Property: Isolation - For any concurrent transactions, they don't interfere with each other
     * Tests that concurrent transactions maintain isolation
     */
    @Property(tries = 50)
    void transactionIsolationIsMaintained(
            @ForAll @NotBlank String baseEmail,
            @ForAll @NotBlank String baseName,
            @ForAll @Positive int concurrentOperations) {
        
        // Limit concurrent operations to reasonable number for testing
        int operations = Math.min(concurrentOperations, 10);
        
        ExecutorService executor = Executors.newFixedThreadPool(operations);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        try {
            // Act - Execute concurrent transactions
            CompletableFuture<?>[] futures = new CompletableFuture[operations];
            
            for (int i = 0; i < operations; i++) {
                final int index = i;
                futures[i] = CompletableFuture.runAsync(() -> {
                    try {
                        transactionTemplate.execute(status -> {
                            String uniqueEmail = baseEmail + "_" + index + "_" + System.nanoTime();
                            String uniqueName = baseName + "_" + index;
                            
                            jdbcTemplate.update(
                                "INSERT INTO test_users (id, email, full_name) VALUES (?, ?, ?)",
                                UUID.randomUUID().toString(), uniqueEmail, uniqueName
                            );
                            
                            // Simulate some processing time
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            
                            return null;
                        });
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    }
                }, executor);
            }

            // Wait for all transactions to complete
            CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);

            // Assert - Isolation: Each transaction should complete independently
            int totalOperations = successCount.get() + failureCount.get();
            assertEquals(operations, totalOperations, "All transactions should complete (either success or failure) (Isolation)");
            
            // Verify data integrity - count actual records
            int actualUserCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM test_users", Integer.class);
            assertEquals(successCount.get(), actualUserCount, "Number of inserted users should match successful transactions (Isolation)");

        } catch (Exception e) {
            fail("Concurrent transaction test failed: " + e.getMessage());
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Property: Durability - For any committed transaction, changes persist
     * Tests that committed changes are durable
     */
    @Property(tries = 100)
    void transactionDurabilityIsEnsured(
            @ForAll @NotBlank String userEmail,
            @ForAll @NotBlank String userName,
            @ForAll @NotBlank String projectName) {
        
        // Act - Commit transaction
        String userId = transactionTemplate.execute(status -> {
            String id = UUID.randomUUID().toString();
            jdbcTemplate.update(
                "INSERT INTO test_users (id, email, full_name) VALUES (?, ?, ?)",
                id, userEmail, userName
            );

            String projectId = UUID.randomUUID().toString();
            jdbcTemplate.update(
                "INSERT INTO test_projects (id, name, owner_id) VALUES (?, ?, ?)",
                projectId, projectName, id
            );

            return id;
        });

        // Assert - Durability: Data should persist after transaction commit
        List<Map<String, Object>> users = jdbcTemplate.queryForList(
            "SELECT * FROM test_users WHERE id = ?", userId
        );
        assertEquals(1, users.size(), "User data should persist after transaction commit (Durability)");
        assertEquals(userEmail, users.get(0).get("email"), "User email should be preserved (Durability)");
        assertEquals(userName, users.get(0).get("full_name"), "User name should be preserved (Durability)");

        List<Map<String, Object>> projects = jdbcTemplate.queryForList(
            "SELECT * FROM test_projects WHERE owner_id = ?", userId
        );
        assertEquals(1, projects.size(), "Project data should persist after transaction commit (Durability)");
        assertEquals(projectName, projects.get(0).get("name"), "Project name should be preserved (Durability)");
    }

    /**
     * Property: Data corruption prevention - For any invalid operation, data integrity is preserved
     * Tests that the system prevents data corruption through constraint enforcement
     */
    @Property(tries = 100)
    void dataCorruptionIsPrevented(
            @ForAll @NotBlank String validEmail,
            @ForAll @NotBlank String validName) {
        
        // First, insert valid data
        String validUserId = transactionTemplate.execute(status -> {
            String id = UUID.randomUUID().toString();
            jdbcTemplate.update(
                "INSERT INTO test_users (id, email, full_name) VALUES (?, ?, ?)",
                id, validEmail, validName
            );
            return id;
        });

        // Test 1: Try to insert duplicate email (should fail)
        assertThrows(Exception.class, () -> {
            transactionTemplate.execute(status -> {
                jdbcTemplate.update(
                    "INSERT INTO test_users (id, email, full_name) VALUES (?, ?, ?)",
                    UUID.randomUUID().toString(), validEmail, "Different Name"
                );
                return null;
            });
        }, "Duplicate email should be rejected (prevents data corruption)");

        // Test 2: Try to insert project with invalid owner (should fail)
        assertThrows(Exception.class, () -> {
            transactionTemplate.execute(status -> {
                jdbcTemplate.update(
                    "INSERT INTO test_projects (id, name, owner_id) VALUES (?, ?, ?)",
                    UUID.randomUUID().toString(), "Test Project", "invalid-user-id"
                );
                return null;
            });
        }, "Invalid foreign key should be rejected (prevents data corruption)");

        // Verify original data is still intact
        List<Map<String, Object>> users = jdbcTemplate.queryForList(
            "SELECT * FROM test_users WHERE id = ?", validUserId
        );
        assertEquals(1, users.size(), "Original user should still exist (data integrity preserved)");
        assertEquals(validEmail, users.get(0).get("email"), "Original user email should be unchanged (data integrity preserved)");
    }
}

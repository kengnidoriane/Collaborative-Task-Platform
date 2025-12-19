import {
  type Tree,
  formatFiles,
  installPackagesTask,
  generateFiles,
  joinPathFragments,
} from '@nx/devkit';

interface ComponentGeneratorSchema {
  name: string;
  project: string;
  directory?: string;
  export?: boolean;
}

export default async function (tree: Tree, options: ComponentGeneratorSchema) {
  const projectRoot = `apps/${options.project}`;
  const componentDir = options.directory || 'components';
  const componentPath = joinPathFragments(projectRoot, componentDir, options.name);

  generateFiles(tree, joinPathFragments(__dirname, 'files'), componentPath, {
    ...options,
    className: options.name.charAt(0).toUpperCase() + options.name.slice(1),
    tmpl: '',
  });

  await formatFiles(tree);
  return () => {
    installPackagesTask(tree);
  };
}

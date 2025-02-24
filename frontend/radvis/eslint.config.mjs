// @ts-check
import eslint from '@eslint/js';
import angular from 'angular-eslint';
import header from 'eslint-plugin-header';
import jasmine from 'eslint-plugin-jasmine';
import eslintPluginPrettierRecommended from 'eslint-plugin-prettier/recommended';
import tseslint from 'typescript-eslint';

// s. https://github.com/Stuk/eslint-plugin-header/issues/57
header.rules.header.meta.schema = false;

export default tseslint.config(
  {
    files: ['**/*.ts'],
    extends: [
      eslint.configs.recommended,
      ...tseslint.configs.stylisticTypeChecked,
      ...tseslint.configs.recommendedTypeChecked,
      ...angular.configs.tsRecommended,
      eslintPluginPrettierRecommended,
      {
        languageOptions: {
          parserOptions: {
            projectService: true,
            tsconfigRootDir: import.meta.dirname,
          },
        },
      },
    ],
    plugins: { jasmine, header },
    processor: angular.processInlineTemplates,
    rules: {
      '@typescript-eslint/no-deprecated': 'warn',
      '@typescript-eslint/no-explicit-any': 'warn',
      '@typescript-eslint/no-unsafe-member-access': 'off',
      '@typescript-eslint/no-floating-promises': 'off',
      '@typescript-eslint/prefer-nullish-coalescing': 'warn',
      '@typescript-eslint/no-unsafe-return': 'off',
      '@typescript-eslint/no-unsafe-assignment': 'off',
      '@typescript-eslint/unbound-method': 'off',
      '@typescript-eslint/consistent-type-assertions': 'warn',
      '@typescript-eslint/no-unsafe-argument': 'off',
      '@typescript-eslint/no-unsafe-enum-comparison': 'off',
      '@typescript-eslint/consistent-indexed-object-style': 'off',
      '@typescript-eslint/no-unsafe-call': 'off',
      'no-constant-binary-expression': 'warn',
      '@typescript-eslint/no-empty-function': 'off',
      '@typescript-eslint/prefer-optional-chain': 'warn',
      'no-extra-boolean-cast': 'off',
      '@typescript-eslint/consistent-generic-constructors': 'off',
      '@typescript-eslint/dot-notation': 'off',
      '@typescript-eslint/no-base-to-string': 'off',
      'no-empty': 'off',
      '@typescript-eslint/restrict-template-expressions': 'off',
      '@typescript-eslint/restrict-plus-operands': 'off',
      '@angular-eslint/prefer-standalone': 'off',
      '@angular-eslint/directive-selector': [
        'error',
        {
          type: 'attribute',
          prefix: 'rad',
          style: 'camelCase',
        },
      ],
      '@angular-eslint/component-selector': [
        'error',
        {
          type: 'element',
          prefix: 'rad',
          style: 'kebab-case',
        },
      ],
      '@typescript-eslint/naming-convention': [
        'error',
        {
          selector: 'enumMember',
          format: ['UPPER_CASE'],
        },
      ],
      'prefer-arrow-callback': [
        'warn',
        {
          allowNamedFunctions: false,
        },
      ],
      '@typescript-eslint/explicit-function-return-type': 'error',
      'no-shadow': 'off',
      '@typescript-eslint/no-shadow': 'error',

      'no-underscore-dangle': [
        'error',
        {
          allowAfterThis: true,
          allowAfterSuper: true,
        },
      ],

      'no-unused-vars': 'warn',
      '@typescript-eslint/no-unused-vars': 'off',
      'jasmine/no-focused-tests': 'error',

      'no-restricted-imports': [
        'error',
        {
          patterns: ['rxjs/internal/*', '\\.\\./\\.\\./'],
        },
      ],

      'header/header': ['error', 'header.js'],
    },
  },
  {
    files: ['**/*.html'],
    plugins: { header },
    extends: [...angular.configs.templateRecommended],
    rules: {},
  },
  {
    files: ['**/*.scss'],
    plugins: { header },
    rules: {
      'header/header': ['error', 'header.js'],
    },
  }
);

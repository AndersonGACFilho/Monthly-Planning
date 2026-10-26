import js from '@eslint/js';
import i18next from 'eslint-plugin-i18next';
import jsdoc from 'eslint-plugin-jsdoc';
import reactHooks from 'eslint-plugin-react-hooks';
import tsdoc from 'eslint-plugin-tsdoc';
import globals from 'globals';
import tseslint from 'typescript-eslint';

export default tseslint.config(
  { ignores: ['dist', 'coverage', 'src/api/generated'] },
  {
    files: ['**/*.{ts,tsx}'],
    extends: [js.configs.recommended, ...tseslint.configs.recommended],
    languageOptions: { globals: globals.browser },
    plugins: { 'react-hooks': reactHooks, tsdoc },
    rules: {
      ...reactHooks.configs.recommended.rules,
      // DOC-06: TSDoc must parse correctly wherever it is written
      'tsdoc/syntax': 'error',
    },
  },
  {
    // I18N-04: no user-visible text written directly in components; use t('key')
    files: ['src/**/*.tsx'],
    ignores: ['src/**/*.test.tsx', 'src/test/**'],
    plugins: { i18next },
    rules: {
      'i18next/no-literal-string': [
        'error',
        {
          mode: 'jsx-text-only',
          'jsx-attributes': { include: ['title', 'alt', 'placeholder', 'aria-label', 'label'] },
        },
      ],
    },
  },
  {
    // DOC-06: shared hooks, components and the API client are documented
    files: ['src/shared/**/*.{ts,tsx}', 'src/api/*.ts', 'src/i18n/*.ts', 'src/app/*.{ts,tsx}'],
    ignores: ['src/**/*.test.{ts,tsx}'],
    plugins: { jsdoc },
    rules: {
      'jsdoc/require-jsdoc': [
        'error',
        { publicOnly: true, require: { FunctionDeclaration: true, ClassDeclaration: true, ArrowFunctionExpression: true } },
      ],
    },
  },
);

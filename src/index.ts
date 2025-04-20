// Reexport the native module. On web, it will be resolved to LetroPassportModule.web.ts
// and on native platforms to LetroPassportModule.ts
export { default } from './LetroPassportModule';
export { default as LetroPassportView } from './LetroPassportView';
export * from  './LetroPassport.types';

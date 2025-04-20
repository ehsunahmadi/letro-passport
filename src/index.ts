// Reexport the native module. On web, it will be resolved to LetroPassportModule.web.ts
// and on native platforms to LetroPassportModule.ts
export * from "./LetroPassport.types";
export { default } from "./LetroPassportModule";

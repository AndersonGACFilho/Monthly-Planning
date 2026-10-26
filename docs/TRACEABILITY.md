# Traceability matrix

SPEC §15: a feature is done only when it has Javadoc, automated tests and its row here. Rows start from the SPEC table and gain classes and tests as features are built. Status: **done**, **partial** (some rules still open) or **planned**.

| Feature | Requirements | Main classes | Tests | Status |
| --- | --- | --- | --- | --- |
| Portable storage | POR-03, POR-04, POR-10, POR-11, IMP-03 | `AppPaths`, `StorageConfiguration`, `WalCheckpointOnShutdown`, `InstanceLock`, `FileNameSanitizer` | `AppPathsTest`, `StorageIT`, `InstanceLockTest`, `FileNameSanitizerTest`, `StartupExitCodeTest` | done |
| API boundary | API-01, API-03, API-07, API-10, API-12, IMP-04, I18N-06, DOC-05 | `RootController`, `ProfileController`, `Relations`, `GlobalExceptionHandler`, `SessionTokenFilter`, `WebConfiguration`, `LocaleConfiguration`, `OpenApiConfiguration` | `RootControllerIT`, `ProfileControllerIT`, `GlobalExceptionHandlerIT`, `SessionTokenFilterIT`, `ApiNamingIT`, `OpenApiContractIT` | done |
| Architecture rules | SOL-05, SOL-06, TST-01, TST-02 | — | `LayerDependencyTest`, `TestConventionsTest` | done |
| Desktop shell | POR-06, POR-08, UI-10, IMP-06 | `lib.rs`, `backend.rs`, `app_home.rs`, `window_state.rs`, `ParentProcessWatcher`, `BackendGate` | Rust unit tests, `ParentProcessWatcherIT`, `BackendGate.test.tsx`, `portable_smoke.py` | partial |
| Interface shell | IMP-05, RNF-06, RNF-10, UI-08, I18N-04, DOC-06 | `AppLayout`, `navigation.ts`, `router.tsx`, `i18n.ts`, `apiFetch` | `App.test.tsx`, `i18n.test.ts`, `http.test.ts`, `generatedClient.test.tsx` | done |
| Billing period | RN-01 to RN-03 | `BillingPeriodPolicy` | `BillingPeriodPolicyTest` | planned |
| Installment rotation | RN-04 to RN-09 | `InstallmentProjector` | `InstallmentProjectorTest`, `ConfirmImportIT` | planned |
| Deduplication | RN-10 to RN-14 | `EntryKeyFactory`, `ImportClassifier` | `EntryKeyFactoryTest`, `ImportClassifierTest` | planned |
| Alias | RF-46 to RF-51 | `DisplayDescriptionResolver`, `RenameEntryUseCase` | `DisplayDescriptionResolverTest`, `BillEntryControllerIT` | planned |
| Itaú import | RF-60 to RF-73 | `ItauPdfParser`, `ItauXlsxParser`, `PreviewImportUseCase` | `ItauPdfParserTest`, `ItauXlsxParserTest`, `import.spec.ts` | planned |
| Overview | RF-10 to RF-17 | `MonthlySummaryService` | `MonthlySummaryServiceTest`, `monthly-summary.spec.ts` | planned |
| Incomes and fixed expenses | RF-21 to RF-24, RF-30 to RF-32 | `GenerateOccurrencesUseCase` | `GenerateOccurrencesUseCaseTest` | planned |
| Split between members | RF-26 | `SplitPolicy` | `SplitPolicyTest` | planned |
| Member statement | RF-80 to RF-94, TPL-01 to TPL-07 | `MemberStatementBuilder`, `SendStatementUseCase` | `MemberStatementBuilderTest`, `SendStatementUseCaseTest` | planned |
| Language | I18N-01 to I18N-14 | `MessageSource`, `locales/*.json` | `MessageKeysConsistencyTest`, `i18n.test.ts`, `i18n.spec.ts` | partial |

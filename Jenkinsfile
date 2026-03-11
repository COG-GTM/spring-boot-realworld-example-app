// GS Managed Pipelines v1 (shared library). See https://go/managed-pipelines (internal).
@Library('gs-managed-pipelines@v1') _

managedPipeline {
    appId        = 'CLT-COLLATERAL-SVC'
    team         = 'collateral-platform'
    jdk          = 'jdk11'
    buildTool    = 'maven'
    goals        = 'clean verify'
    clmStage     = 'build'          // CLM policy scan runs after the build
    artifactRepo = 'gs-maven-releases'
    deploy {
        environments = ['dev', 'qa']
        strategy     = 'rolling'
    }
    notify {
        slack = '#collateral-platform-builds'
    }
}

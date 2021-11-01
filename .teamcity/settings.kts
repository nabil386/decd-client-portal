package OasUnlockBot_BdmDev.buildTypes

import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.dockerCommand
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs

object OasUnlockBot_DeployBdmDev : BuildType({
    id = AbsoluteId("OasUnlockBot_DeployBdmDev")
    name = "Deploy - BDM Dev"
    
    allowExternalStatus = true
    
    params {
        param("env.RG_DEV", "ESdCDPSBDMK8SDev")
        param("env.K8S_CLUSTER_NAME", "ESdCDPSBDMK8SDev-K8S")
        param("env.KEYVAULT_NAME", "bdmDevDPSKeyVault")
        param("env.TENANT_ID", "%vault:dts-sre/azure!/tenant-id% ")
        param("env.DOCKER_TAG", "%build.number%")
        param("env.RBAC_TEAM_ID", "ad5a7b36-9bdb-4c6e-a549-9ec2ef52041e")
        param("env.TARGET", "dev")
        param("env.BRANCH", "%teamcity.build.branch%")
        param("env.KEYVAULT_READ_PASSWORD", "%vault:dps-bdm-dev/depot-keyvault!/depot-keyvault-sp-pass% ")
        param("env.BASE_DOMAIN", "bdm-dev.dts-stn.com")
        param("env.SUBSCRIPTION", "%vault:dts-sre/azure!/decd-dev-subscription-id% ")
        param("env.SUBSCRIPTION_ID", "%vault:dps-bdm-dev/azure!/dev-subscription-id% ")
        param("env.KEYVAULT_READ_USER", "%vault:dps-bdm-dev/depot-keyvault!/depot-keyvault-sp-id%")
        param("env.BASE_DOMAIN_DEV", "bdm-dev.dts-stn.com")
        param("env.PROJECT", "dts-oas-callback-bot")
        param("env.K8S_RG_NAME", "ESdCDPSBDMK8SDev")
    }
    
    vcs {
        root(AbsoluteId("OasUnlockBot_HttpsGithubComDtsStnOasUnblockBotRefsHeadsMain"))
    }
    
    steps {
        dockerCommand {
            name = "Build Docker Container"
            commandType = build {
                source = file {
                    path = "Dockerfile.dev"
                }
                namesAndTags = "%env.ACR_DOMAIN%/%env.PROJECT%:%env.DOCKER_TAG%"
                commandArgs = "--pull --build-arg BUILD_DATE=%system.build.start.date% --build-arg TC_BUILD=%build.number%"
            }
        }
        script {
            name = "Log In To Azure"
            scriptContent = """
                az login --service-principal -u %TEAMCITY_USER% -p %TEAMCITY_PASS% --tenant %env.TENANT-ID%
                az account set -s %env.SUBSCRIPTION%
                az acr login -n MTSContainers
            """.trimIndent()
        }
        dockerCommand {
            name = "Push Image to ACR"
            commandType = push {
                namesAndTags = "%env.ACR_DOMAIN%/%env.PROJECT%:%env.DOCKER_TAG%"
            }
        }
        script {
            name = "Helmfile Deploy"
            scriptContent = """
                cd ./helmfile/scripts
                ./apply.sh
            """.trimIndent()
        }
    }
    
    triggers {
        vcs {
            branchFilter = """
                +:*
                -:<merge*>
            """.trimIndent()
        }
    }
})
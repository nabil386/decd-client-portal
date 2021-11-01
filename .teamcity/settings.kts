package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.dockerCommand
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs

object BuildAndDeploy : BuildType({
    name = "Build and Deploy"

    vcs {
        root(HttpsGithubComNabil386decdClientPortalRefsHeadsMain)
    }

steps {
    dockerCommand {
        name = "Build"
        commandType = build {
            source = file {
                path = "Dockerfile"
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
        }
    }
})
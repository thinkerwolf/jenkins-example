pipeline {
    agent {
        label 'example'
    }

    parameters {
        nexusArtifactChoices(name: 'DEPLOY_ARTIFACTS', repository: 'raw-op', serverId: 'nexus', visibleItemCount: 20,
                groupIdArtifactIds: '''
                                tech.bitkernel.devops:gradle-example
                                tech.bitkernel.devops:maven-example
                                ''')
    }

    stages {

        stage('Config') {
            steps {
                script {
                    def schemaNameMap = ["gradle-example": "test", "maven-example": "test_green"]
                    def databaseApps = []
                    def nacosAlterMap = [:]
                    def appList = "${DEPLOY_ARTIFACTS}".split(',')
                    appList.each { app ->
                        def gav = "${app}".split(':')
                        def groupId = gav[0]
                        def artifactId = gav[1]
                        def version = gav[2]

                        def start = true
                        try {
                            nexusArtifactDownload(
                                    serverId: 'nexus',
                                    repository: 'raw-op',
                                    groupId: groupId,
                                    artifactId: artifactId,
                                    version: version,
                                    location: "${artifactId}/changelog.tar.gz")
                        } catch (Exception e) {
                            echo "Caught: ${e}"
                            start = false
                        }

                        if (start) {
                            sh """
                            tar -xvf ${artifactId}/changelog.tar.gz -C ${artifactId}
                            """
                            if (fileExists("${artifactId}/changelog/nacos")) {
                                def nacosChangeSet = nacosChangeSetGet(
                                        nacosId: 'vod-dev',
                                        changeLogFile: "${artifactId}/changelog/nacos/changelog-root.yaml",
                                        vars: ['NAMESPACE': 'blue', 'ENV': 'sit']
                                )

                                def changeSetIds = nacosChangeSet['ids']
                                if (changeSetIds && changeSetIds.size() > 0) {
                                    nacosAlterMap[artifactId] = ['parameter': nacosConfigAlter(items: nacosChangeSet['changes']), 'changeSetIds': changeSetIds]
                                } else {
                                    echo "No nacos change set"
                                }
                            }

                            if (fileExists("${artifactId}/changelog/database")) {
                                databaseApps.add(artifactId)
                            }
                        }
                    }

                    nacosAlterMap.each { entry ->
                        def changeSetIds = entry.value['changeSetIds']
                        def parameter = entry.value['parameter']
                        def alterResult = input(message: "Nacos Config Edit: ${entry.key}", parameters: [parameter])
                        println(alterResult)
                        // nacosChangeSetApply(nacosId: "vod-dev", changeSetId: changeSetIds, items: alterResult['values'])
                    }

                    // 刷数据库
                    databaseApps.each { artifactId ->
                        def schemaName = schemaNameMap[artifactId]
                        liquibaseRawCmd(databaseId: "optnet-sit", cwd: "${artifactId}/changelog/database", command: 'update-sql', args: "--changelog-file=changelog-root.yaml --default-schema-name=${schemaName}")
                        // liquibaseRawCmd(databaseId: "optnet-sit", cwd: "${artifactId}/changelog/database", command: 'update', args: "--changelog-file=changelog-root.yaml --default-schema-name=${schemaName}")
                    }
                }
            }
        }
    }
}

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
                    def schemaNameMap = ["gradle-example": "test_blue", "maven-example": "test_green"]
                    def appList = "${DEPLOY_ARTIFACTS}".split(',')
                    parallelStages = [:]
                    appList.each { app ->
                        def gav = "${app}".split(':')
                        def groupId = gav[0]
                        def artifactId = gav[1]
                        def version = gav[2]
                        parallelStages[artifactId] = {

                            nexusArtifactDownload(
                                    serverId: 'nexus',
                                    repository: 'raw-op',
                                    groupId: groupId,
                                    artifactId: artifactId,
                                    version: version,
                                    location: 'changelog.tar.gz')

                            sh 'tar -xvf changelog.tar.gz'

                            // 执行Nacos配置变更，读取 changeLog 文件，获取changeSet
                            def nacosChangeSet = nacosChangeSetGet(nacosId: 'vod-dev', changeLogFile: 'changelog/nacos/changelog-root.yaml')
                            def changeSetIds = nacosChangeSet['ids']
                            if (changeSetIds && changeSetIds.size() > 0) {
                                // 弹出界面预览配置前后修改比对
                                def alterResult = input(message: 'Nacos Config Edit', parameters: [nacosConfigAlter(items: nacosChangeSet['changes'])])
                                // 应用 changeSet，刷入nacos配置
                                nacosChangeSetApply(nacosId: "vod-dev", changeSetId: changeSetIds, items: alterResult['values'])
                            } else {
                                echo "No nacos change set"
                            }

                            // liquibase update
                            def schemaName = schemaNameMap[artifactId]
                            liquibaseRawCmd(databaseId: "optnet-sit", cwd: 'changelog/database', command: 'update-sql', args: "--changelog-file=changelog-root.yaml --default-schema-name=${schemaName}")
                            liquibaseRawCmd(databaseId: "optnet-sit", cwd: 'changelog/database', command: 'update', args: "--changelog-file=changelog-root.yaml --default-schema-name=${schemaName}")
                        }
                    }

                    parallel parallelStages
                }
            }
        }
    }
}

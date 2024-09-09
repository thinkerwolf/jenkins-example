pipeline {
    agent {
        label 'example'
    }

    parameters {
        nexusArtifactChoices(name: 'DEPLOY_ARTIFACTS', repository: 'raw-op', serverId: 'nexus', visibleItemCount: 20,
                groupIdArtifactIds: '''
                                tech.bitkernel.devops:gradle-example
                                ''')
    }

    environment {
        NEXUS_REPO = 'raw-op'
    }

    stages {
        stage('Config') {
            steps {
                script {
                    def appList = "${DEPLOY_ARTIFACTS}".split(',')
                    appList.each { app ->
                        stage("Deploy ${app}") {
                            script {
                                def gav = "${app}".split(':')
                                def groupId = gav[0]
                                def artifactId = gav[1]
                                def version = gav[2]
                                nexusArtifactDownload(
                                        serverId: 'nexus',
                                        repository: "${NEXUS_REPO}",
                                        groupId: groupId,
                                        artifactId: artifactId,
                                        version: version,
                                        location: 'changelog.tar.gz')

                                sh 'tar -xvf changelog.tar.gz'

                                // 执行Nacos配置变更，读取 changeLog 文件，获取changeSet
                                def nacosChangeSet = nacosChangeSetGet(nacosId: 'vod-dev', changeLogFile: 'changelog/nacos/changelog-root.yaml')

                                if (nacosChangeSet["id"]) {
                                    // 弹出界面预览配置前后修改比对
                                    def alterResult = input(message: 'Nacos Config Edit', parameters: [nacosConfigAlter(items: nacosChangeSet['changes'])])
                                    // 应用 changeSet，刷入nacos配置
                                    nacosChangeSetApply(nacosId: "vod-dev", changeSetId: nacosChangeSet['id'], items: alterResult['values'])
                                } else {
                                    echo "No nacos change set"
                                }

                                // liquibase update
                                liquibaseUpdate(databaseId: "optnet-sit", file: "changelog/database/changelog-root.yaml")

                            }
                        }
                    }
                }
            }
        }
    }

}
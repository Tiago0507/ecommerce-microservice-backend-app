pipeline {
  agent none

  options {
    timestamps()
    ansiColor('xterm')
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '10'))
  }

  stages {
    stage('Dev Pipeline (develop)') {
      when {
        branch 'develop'
      }
      agent { label 'maven-dind' }
      environment {
        DOCKER_CLI_EXPERIMENTAL = 'enabled'
        DOCKER_HOST = 'tcp://localhost:2375'
        COMPOSE_DOCKER_CLI_BUILD = '1'
        DOCKER_BUILDKIT = '1'
      }
      stages {
        stage('Checkout') {
          steps {
            checkout scm
          }
        }
        stage('Build (Maven)') {
          steps {
            container('maven') {
              sh 'mvn -q -ntp -DskipTests clean package'
            }
          }
        }
        stage('Bring up dev stack (Docker Compose)') {
          steps {
            container('docker') {
              sh '''
                set -euxo pipefail
                docker version
                docker compose version
                # Start core services first, then business services
                docker compose -f core.yml up -d
                docker compose -f compose.yml up -d
              '''
            }
          }
        }
        stage('Smoke tests') {
          steps {
            container('docker') {
              sh 'chmod +x scripts/smoke-test.sh && scripts/smoke-test.sh'
            }
          }
        }
      }
      post {
        always {
          container('docker') {
            sh '''
              set +e
              docker compose -f compose.yml logs --no-color > compose.logs || true
              docker compose -f core.yml logs --no-color > core.logs || true
            '''
          }
          archiveArtifacts allowEmptyArchive: true, artifacts: 'core.logs, compose.logs'
        }
        cleanup {
          container('docker') {
            sh '''
              set +e
              docker compose -f compose.yml down -v || true
              docker compose -f core.yml down -v || true
            '''
          }
        }
      }
    }

    stage('Stage Pipeline (placeholder)') {
      when { branch 'stage' }
      agent { label 'maven-dind' }
      steps {
        echo 'Stage pipeline placeholder: deploy to K8s + E2E + Locust (se implementará en el siguiente hito)'
      }
    }

    stage('Prod Pipeline (placeholder)') {
      when { branch 'master' }
      agent { label 'maven-dind' }
      steps {
        echo 'Master pipeline placeholder: release notes + tag + deploy prod en K8s (se implementará en el siguiente hito)'
      }
    }
  }
}

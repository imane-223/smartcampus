
pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                echo 'Preparation du code source SmartCampus'
            }
        }

        stage('Build') {
            steps {
                bat 'mvnw.cmd -B -DskipTests clean package'
            }
        }

        stage('Test') {
            steps {
                bat 'mvnw.cmd -B test'
            }
        }
    }
}

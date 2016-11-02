// -*- mode: groovy; -*-

node {
  currentBuild.result = "SUCCESS"

  try {
    stage 'Checkout'
      checkout scm

    stage 'Test'
      sh "boot --no-colors ci"

    stage 'Build'
      sh "boot --no-colors build -e dev-builds"

    stage 'Deploy'
      sh "./bin/deploy -b dev.zetawar.com"

  } catch (err) {
    currentBuild.result = "FAILURE"
    throw err
  } finally {
    notifyBuild(currentBuild.result)
  }
}

def notifyBuild(String buildStatus = 'STARTED') {
  // build status of null means successful
  buildStatus =  buildStatus ?: 'SUCCESSFUL'

  // Default values
  def colorName = 'RED'
  def colorCode = '#FF0000'
  def recipients = UNSUCCESSFUL_RECIPIENTS
  def subject = "${env.JOB_NAME} - Build # ${env.BUILD_NUMBER} - ${buildStatus}!"
  def summary = "${subject} (${env.BUILD_URL})"
  def details = """${env.JOB_NAME} - Build # ${env.BUILD_NUMBER} - ${buildStatus}

Check console output at ${env.BUILD_URL} to view the results.
"""

  // Override default values based on build status
  if (buildStatus == 'STARTED') {
    color = 'YELLOW'
    colorCode = '#FFFF00'
  } else if (buildStatus == 'SUCCESSFUL') {
    color = 'GREEN'
    colorCode = '#00FF00'
    recipients = SUCCESSFUL_RECIPIENTS
  } else {
    color = 'RED'
    colorCode = '#FF0000'
  }

  // Send notifications
  emailext (
    to: recipients,
    replyTo: env.ZETAWAR_REPLY_TO,
    subject: subject,
    body: details,
  )
}

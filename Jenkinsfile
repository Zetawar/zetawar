# -*- mode: groovy; -*-

node {
  currentBuild.result = "SUCCESS"

  try {
    stage 'Checkout'
      checkout scm

    stage 'Test'
      sh "boot --no-colors ci"

    stage 'Build'
      sh "boot build -e dev-builds"

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
  def recipients = env.ZETAWAR_UNSUCCESSFUL_RECIPIENTS
  def subject = "${buildStatus}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'"
  def summary = "${subject} (${env.BUILD_URL})"
  def details = """<p>STARTED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
    <p>Check console output at &QUOT;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>&QUOT;</p>"""

  // Override default values based on build status
  if (buildStatus == 'STARTED') {
    color = 'YELLOW'
    colorCode = '#FFFF00'
  } else if (buildStatus == 'SUCCESSFUL') {
    color = 'GREEN'
    colorCode = '#00FF00'
    recipients = env.ZETAWAR_SUCCESSFUL_RECIPIENTS
  } else {
    color = 'RED'
    colorCode = '#FF0000'
  }

  // Send notifications
  emailext (
      subject: subject,
      body: details,
      to: recipients
    )
}

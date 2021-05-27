plugins {
    id("com.github.psxpaul.execfork")
}

tasks.create("pingPong") {
    val pingServer = tasks.getByPath("ping:startAllTestsServer") as com.github.psxpaul.task.JavaExecFork
    val pongServer = tasks.getByPath("pong:startAllTestsServer") as com.github.psxpaul.task.JavaExecFork

    dependsOn("pong:build", "ping:build")

    group = "verification"

    doLast {
        pingServer.exec()
        pongServer.exec()

        if (pingServer.process?.waitFor() != 0)
            error("Ping server failed")

        if (pongServer.process?.waitFor() != 0)
            error("Ping server failed")

    }
}

tasks.create("allTests") {
    group = "verification"
    dependsOn("pingPong")
}
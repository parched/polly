function fn() {
    const serverCommand = karate.env;
    karate.log('karate.env system property was:', serverCommand);
    if (serverCommand) {
        const server = karate.fork(serverCommand);
        // TODO: wait for server up
        karate.configure('afterScenario', () => {
            server.close(false);
        });
        karate.waitForHttp('http://localhost:8080')
    }

    return {};
}
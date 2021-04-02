function fn() {
    const port = 7000

    const serverCommand = karate.env;
    karate.log('karate.env system property was:', serverCommand);
    if (serverCommand) {
        const server = karate.fork(serverCommand + ' ' + port);
        // TODO: wait for server up
        karate.configure('afterScenario', () => {
            server.close(false);
        });
        karate.waitForHttp('http://localhost:' + port)
    }

    return {};
}
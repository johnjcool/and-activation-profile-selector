AND Profile Activation Maven Extension
-------------------------------------

According to the documentation at http://www.sonatype.com/books/mvnref-book/reference/profiles-sect-activation.html a profile is activated when all activation conditions are met.

Ticket: http://jira.codehaus.org/browse/MNG-4565

In order to activate extension, you cannot include it into ```<build><extensions>``` element, because profile activation is done
before it would be activated. So you need to copy following files into *$MAVEN_HOME/lib/ext*:

* and-profile-activator-extension.jar (available in target directory)

Now you can write the following in your pom.xml:

    <profile>
        <id>stage-server-integration-test</id>    
        <activation>
            <property>
                <name>stage</name>
                <value>integration-test</value>
            </property>
			<!-- and linking -->
			<file>
				<exists>${basedir}/src/test/server/server.properties</exists>
			<file>
        </activation>
    </profile>

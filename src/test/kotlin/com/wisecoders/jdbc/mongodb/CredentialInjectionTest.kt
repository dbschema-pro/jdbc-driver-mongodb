package com.wisecoders.jdbc.mongodb

import com.mongodb.AuthenticationMechanism
import com.mongodb.ConnectionString
import com.mongodb.MongoCredential
import com.mongodb.MongoTimeoutException
import com.wisecoders.jdbc.mongodb.wrappers.WrappedMongoClient
import java.sql.SQLException
import java.util.Properties
import org.assertj.core.api.WithAssertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * DbSchema passes username/password as JDBC properties, not inside the URL. The
 * "Cloud - SCRAM-SHA-256 Authentication" method produces URLs with an authMechanism
 * parameter and no userinfo; parsing such a URL must not fail with "username can not be null".
 */
class CredentialInjectionTest : WithAssertions {

    @Test
    fun `client is created when the url specifies an auth mechanism and credentials come from properties`() {
        val client = WrappedMongoClient(
            "mongodb://localhost:47017/mydb?authMechanism=SCRAM-SHA-256&serverSelectionTimeoutMS=100",
            properties(user = "myuser", password = "mypassword"),
            "mydb",
            ScanStrategy.fast,
            false,
            false,
        )

        client.close()
    }

    /**
     * A full connect needs a live server (the connection pings it), so this asserts the failure
     * is the network attempt — not the former "username can not be null" parse error.
     */
    @Test
    fun `connect reaches the server instead of failing to parse the url`() {
        assertThatThrownBy {
            JdbcDriver().connect(
                "jdbc:mongodb://localhost:47017/mydb?authMechanism=SCRAM-SHA-256&serverSelectionTimeoutMS=100",
                properties(user = "myuser", password = "mypassword"),
            )
        }
            .isInstanceOf(SQLException::class.java)
            .hasCauseInstanceOf(MongoTimeoutException::class.java)
    }

    @Nested
    inner class UrlLeftUnchanged {

        @Test
        fun `without a user property`() {
            val url = "mongodb://localhost:27017/mydb?authSource=admin"

            assertThat(WrappedMongoClient.injectCredentials(url, null)).isEqualTo(url)
            assertThat(WrappedMongoClient.injectCredentials(url, Properties())).isEqualTo(url)
        }

        @Test
        fun `with an empty user property`() {
            val url = "mongodb://localhost:27017/mydb"

            assertThat(WrappedMongoClient.injectCredentials(url, properties(user = "", password = "secret")))
                .isEqualTo(url)
        }
    }

    @Nested
    inner class InjectedCredential {

        @Test
        fun `auth mechanism from the url is applied`() {
            val credential: MongoCredential = parsedCredential(
                "mongodb://localhost:27017/mydb?authMechanism=SCRAM-SHA-256",
                user = "myuser",
                password = "mypassword",
            )

            assertThat(credential.authenticationMechanism).isEqualTo(AuthenticationMechanism.SCRAM_SHA_256)
            assertThat(credential.userName).isEqualTo("myuser")
            assertThat(String(credential.password!!)).isEqualTo("mypassword")
        }

        @Test
        fun `auth source defaults to the database from the path`() {
            val credential: MongoCredential = parsedCredential(
                "mongodb://localhost:27017/mydb",
                user = "myuser",
                password = "mypassword",
            )

            assertThat(credential.source).isEqualTo("mydb")
        }

        @Test
        fun `authSource parameter wins over the path database`() {
            val credential: MongoCredential = parsedCredential(
                "mongodb://localhost:27017/mydb?authMechanism=SCRAM-SHA-256&authSource=other",
                user = "myuser",
                password = "mypassword",
            )

            assertThat(credential.source).isEqualTo("other")
        }

        @Test
        fun `auth source defaults to admin without a database in the path`() {
            val credential: MongoCredential = parsedCredential(
                "mongodb://localhost:27017",
                user = "myuser",
                password = "mypassword",
            )

            assertThat(credential.source).isEqualTo("admin")
        }

        @Test
        fun `properties replace credentials embedded in the url`() {
            val credential: MongoCredential = parsedCredential(
                "mongodb://urluser:urlpassword@localhost:27017/mydb",
                user = "propuser",
                password = "proppassword",
            )

            assertThat(credential.userName).isEqualTo("propuser")
            assertThat(String(credential.password!!)).isEqualTo("proppassword")
        }

        @Test
        fun `missing password property becomes an empty password`() {
            val credential: MongoCredential = parsedCredential(
                "mongodb://localhost:27017/mydb",
                user = "myuser",
                password = null,
            )

            assertThat(credential.password).isEmpty()
        }

        @Test
        fun `srv url keeps the single host`() {
            val injected: String = WrappedMongoClient.injectCredentials(
                "mongodb+srv://cluster0.example.com/mydb?authMechanism=SCRAM-SHA-256",
                properties(user = "myuser", password = "mypassword"),
            )

            assertThat(injected)
                .isEqualTo("mongodb+srv://myuser:mypassword@cluster0.example.com/mydb?authMechanism=SCRAM-SHA-256")
            assertThat(ConnectionString(injected).hosts).containsExactly("cluster0.example.com")
        }
    }

    @Nested
    inner class SpecialCharacters {

        @ParameterizedTest
        @ValueSource(strings = ["p@ssword", "pa:ss/word?", "pass word", "pass+word", "pass%word", "pa#ss&word", "paßwörd"])
        fun `password survives the round trip through the parser`(password: String) {
            val credential: MongoCredential = parsedCredential(
                "mongodb://localhost:27017/mydb?authMechanism=SCRAM-SHA-256",
                user = "myuser",
                password = password,
            )

            assertThat(String(credential.password!!)).isEqualTo(password)
        }

        @Test
        fun `username survives the round trip through the parser`() {
            val credential: MongoCredential = parsedCredential(
                "mongodb://localhost:27017/mydb",
                user = "user@example.com",
                password = "mypassword",
            )

            assertThat(credential.userName).isEqualTo("user@example.com")
        }
    }

    private fun parsedCredential(
        url: String,
        user: String,
        password: String?,
    ): MongoCredential {
        val injected: String = WrappedMongoClient.injectCredentials(url, properties(user, password))
        val credential: MongoCredential? = ConnectionString(injected).credential
        assertThat(credential).isNotNull()
        return credential!!
    }

    private fun properties(
        user: String?,
        password: String?,
    ): Properties {
        val properties = Properties()
        if (user != null) {
            properties.setProperty("user", user)
        }
        if (password != null) {
            properties.setProperty("password", password)
        }
        return properties
    }
}

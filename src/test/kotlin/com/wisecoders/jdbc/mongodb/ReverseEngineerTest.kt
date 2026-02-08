package com.wisecoders.jdbc.mongodb

import com.wisecoders.common_jdbc.jvm.sql.printResultSet
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import org.assertj.core.api.WithAssertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("disabled until we figure out how to run tests needing docker containers")
class ReverseEngineerTest : WithAssertions {
    private var con: Connection? = null

    @BeforeEach
    @Throws(ClassNotFoundException::class, SQLException::class)
    fun setUp() {
        Class.forName("com.wisecoders.jdbc.mongodb.JdbcDriver")
        val _con = DriverManager.getConnection(urlWithoutAuth, null, null)
        this.con = _con
        val stmt = _con.createStatement()
        stmt.execute("local.words.drop();")
        stmt.execute("local.words.insertOne({word: 'sample', qty:2, prop: [{ category:'verb'},{ base:'latin'}]});")
        stmt.execute("local.words.createIndex( { word: 1, 'prop.category':1 }, {name:'sampleIndex'} );")
        stmt.execute("use tournament;")
        stmt.execute("tournament.students.drop();")
        stmt.execute(
            """tournament.createCollection('students', {
   validator: {
      ${"$"}jsonSchema: {
         bsonType: 'object',
         required: [ 'name', 'year', 'major', 'address' ],
         properties: {
            name: {
               bsonType: 'string',
               description: 'must be a string and is required'
            },
            year: {
               bsonType: 'int',
               minimum: 2017,
               maximum: 3017,
               description: 'must be an integer in [ 2017, 3017 ] and is required'
            },
            major: {
               enum: [ 'Math', 'English', 'Computer Science', 'History', null ],
               description: 'can only be one of the enum values and is required'
            },
            gpa: {
               bsonType: [ 'double' ],
               description: 'must be a double if the field exists'
            },
            address: {
               bsonType: 'object',
               required: [ 'city' ],
               properties: {
                  street: {
                     bsonType: 'string',
                     description: 'must be a string if the field exists'
                  },
                  city: {
                     bsonType: 'string',
                     description: 'must be a string and is required'
                  }
               }
            }
         }
      }
   }
})"""
        )
        stmt.execute("tournament.contacts.drop();")
        stmt.execute(
            """tournament.createCollection( 'contacts',
   { validator: { ${"$"}or:
      [
         { phone: { ${"$"}type: 'string' } },
         { email: { ${"$"}regex: "@mongodb\.com$" } },
         { status: { ${"$"}in: [ 'Unknown', 'Incomplete' ] } }
      ]
   }
} )"""
        )


        stmt.execute("db.master.drop()")
        stmt.execute("db.slave.drop()")

        stmt.execute("db.master.insert( { _id: 1, item: \"box1\", qty: 21 } )")
        stmt.execute("db.master.insert( {  item: \"box3\", qty: 23 } )")


        stmt.execute(
            """var itr = db.master.find().iterator()
var oid = itr.next().get('_id')
print("ObjectId rec1 " + oid)
db.slave.insert( { name: "slave1", master_id: oid } )
var oid = itr.next().get('_id')
print("ObjectId rec2 " + oid)
db.slave.insert( { name: "slave2", master_id: oid } )
"""
        )

        stmt.close()
    }

    @Test
    @Throws(SQLException::class)
    fun testReverseEngineer() {
        val rs = con!!.metaData.getTables("tournament", "tournament", null, null)
        while (rs.next()) {
            val colName = rs.getString(3)
            con!!.metaData.getColumns("local", "local", colName, null).printResultSet()
            con!!.metaData.getColumns("tournament", "local", colName, null).printResultSet()
            con!!.metaData.getIndexInfo("local", "local", colName, false, false).printResultSet()
        }
        val rsf = con!!.metaData.getExportedKeys("tournament", "tournament", null)
        while (rsf.next()) {
            val colName = rsf.getString(3)
        }
    }


    companion object {
        private const val urlWithAuth =
            "mongodb://admin:fictivpwd@localhost:27017/local?authSource=local&connectTimeoutMS=1000"
        private const val urlWithoutAuth = "mongodb://localhost"
    }
}

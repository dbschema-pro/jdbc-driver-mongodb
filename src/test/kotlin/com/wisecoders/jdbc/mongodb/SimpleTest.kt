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
class SimpleTest : WithAssertions {
    private var con: Connection? = null

    @BeforeEach
    @Throws(ClassNotFoundException::class, SQLException::class)
    fun setUp() {
        Class.forName("com.wisecoders.jdbc.mongodb.JdbcDriver")
        val _con = DriverManager.getConnection(urlWithAuth, null, null)
        this.con = _con
        val stmt = _con.createStatement()
        stmt.execute("local.books.drop();")
        stmt.execute("local.booksView.drop();")
        stmt.execute("local.books.insertOne({name: 'Java', qty:2});")
        stmt.execute("local.books.insertOne({name: 'Python', qty:5});")
        stmt.execute("local.books.insertOne({name: 'C++', qty:15});")
        stmt.execute("local.createView('booksView','books', [{ \$project: { 'bookName': '\$name', qty: 1 }}] )")
        stmt.close()
    }

    @Test
    @Throws(Exception::class)
    fun testListDatabases() {
        val stmt = con!!.createStatement()
        stmt.executeQuery("local.listCollectionNames()").printResultSet()
        stmt.close()
    }

    @Test
    @Throws(Exception::class)
    fun testGetViewSource() {
        val stmt = con!!.createStatement()
        stmt.executeQuery("local.getViewSource('booksView')").printResultSet()
        stmt.close()
    }

    @Test
    @Throws(Exception::class)
    fun testFind() {
        val stmt = con!!.createStatement()
        stmt.executeQuery("local.words.find()").printResultSet()
        stmt.close()
    }

    @Test
    @Throws(Exception::class)
    fun testFindRegEx() {
        val stmt = con!!.createStatement()
        stmt.executeQuery("local.words.find(/^J/)").printResultSet()
        stmt.close()
    }

    @Test
    @Throws(Exception::class)
    fun testInsert2() {
        val stmt = con!!.createStatement()

            stmt.executeQuery(
                """local.cities.drop();local.cities.insert(
{ 'country_id' : 'USA', 
    'city_name' : 'San Francisco', 
    'brother_cities' : [
        'Urban', 'Paris'
    ], 
    'suburbs' : [
         {
            'name' : 'Scarsdale'
         }, 
        {
            'name' : 'North Hills'
        } ]
    })"""
            ).printResultSet()
        con!!.commit()
        stmt.executeQuery("local.cities.find()").printResultSet()
        stmt.close()
    }

    @Test
    @Throws(Exception::class)
    fun testFindAnd() {
        val stmt = con!!.createStatement()
        stmt.executeQuery("local.books.find({ \$and: [ {'name':'Java'}, {'qty':2} ] } )").printResultSet()
        stmt.close()
    }

    @Test
    @Throws(Exception::class)
    fun testCount() {
        val stmt = con!!.createStatement()
        stmt.executeQuery("local.books.count()").printResultSet()
        stmt.close()
    }

    @Test
    @Throws(Exception::class)
    fun testDBRef() {
        val stmt = con!!.createStatement()
        stmt.executeQuery("local.bicycles.insertOne( { 'name' : 'city bike', 'bike_colour' : DBRef( 'colours', 'Bla') } ); ").printResultSet()
        stmt.close()
    }

    @Test
    @Throws(Exception::class)
    fun testFindAndOr() {
        val stmt = con!!.createStatement()
        stmt.executeQuery("local.books.find({ \$or: [{'name': 'Java'}, {'name': 'C++' }]})").printResultSet()
        stmt.close()
    }

    @Test
    @Throws(Exception::class)
    fun testUpdate() {
        val stmt = con!!.createStatement()
        stmt.executeQuery("local.books.update({'name':'Java'},{\$set:{'name':'OpenJDK'}})").printResultSet()
        stmt.close()
    }


    @Test
    @Throws(Exception::class)
    fun testFindId() {
        /*
        BasicDBObject query = new BasicDBObject();
        query.put("_id", new ObjectId("5dd593595f94074908de3db9"));
        printResultSet( new ResultSetIterator(((MongoConnection)con).client.getDatabase("local").getCollection("products").find( query), true));
*/
        val stmt = con!!.createStatement()
        stmt.executeQuery("local.books.find({_id:'5facdca7fea0441ab001f51d'})").printResultSet()
        stmt.close()
    }

    @Test
    @Throws(Exception::class)
    fun testInsert() {
        val stmt = con!!.createStatement()
        stmt.executeQuery("local.persons.insert({ 'firstname' : 'Anna', 'lastname' : 'Pruteanu' })").printResultSet()
        stmt.close()
    }

    @Test
    @Throws(Exception::class)
    fun testInsertMany() {
        val stmt = con!!.createStatement()
        stmt.executeQuery("local.testMany.insertMany( [{ 'hello' : '', 'qty454' : 0 }, { 'hello' : '', 'qty454' : 0 }])").printResultSet()
        stmt.close()
    }

    @Test
    @Throws(Exception::class)
    fun testISODate() {
        val stmt = con!!.createStatement()
        stmt.executeQuery("local.testISODate.insert({'shopId':'baaacd90d36e11e9adb40a8baad32c5a','date':ISODate('2019-12-25T07:23:18.408Z')})").printResultSet()
        stmt.close()
    }

    @Test
    @Throws(Exception::class)
    fun testFindGt() {
        val stmt = con!!.createStatement()
        stmt.executeQuery("local.books.find( {qty:{\$gt: 4}})").printResultSet()
        stmt.close()
    }

    @Test
    @Throws(Exception::class)
    fun testOID() {
        val stmt = con!!.createStatement()
        stmt.execute("local.testObjectID.drop();")
        stmt.executeQuery("local.testObjectID.insert({'_id':ObjectId('5e95cfecdfa8c111a4b2a53a'), 'name':'Lulu2'})").printResultSet()
        stmt.close()
    }

    @Test
    @Throws(Exception::class)
    fun testAggregate() {
        val stmt = con!!.createStatement()
        for (str in aggregateScript) {
            stmt.executeQuery(str).printResultSet()
        }
    }

    @Test
    @Throws(Exception::class)
    fun testAggregte2() {
        val stmt = con!!.createStatement()
        stmt.execute("use local")
            stmt.executeQuery(
                """db.orders.aggregate([
 { ${"$"}match: { status: "A" } },
 { ${"$"}group: { _id: "${"$"}cust_id", total: { ${"$"}sum: "${"$"}amount" } } },
 { ${"$"}sort: { total: -1 } }
]);"""
            ).printResultSet()
        stmt.close()
    }

    @Test
    @Throws(Exception::class)
    fun testInsert3() {
        val st = con!!.createStatement()
        st.execute("local.getCollection('issue2').insert({ 'name' : 'aaa' })")
        st.close()
    }

    companion object {
        private const val urlWithAuth =
            "mongodb://localhost:27017/local?scan=fast&authSource=local&connectTimeoutMS=1000"
        private const val urlWithoutAuth = "mongodb://localhost"


        private val aggregateScript = arrayOf(
            "db.food.drop();",
            """db.food.insert([
   { category: 'cake', type: 'chocolate', qty: 10 },
   { category: 'cake', type: 'ice cream', qty: 25 },
   { category: 'pie', type: 'boston cream', qty: 20 },
   { category: 'pie', type: 'blueberry', qty: 15 }
]);""",
            "db.food.createIndex( { qty: 1, type: 1 } );",
            "db.food.createIndex( { qty: 1, category: 1 } );",
            "db.food.aggregate( [ { \$sort: { qty: 1 }}, { \$match: { category: 'cake', qty: 10  } }, { \$sort: { type: -1 } } ] ) "
        )
    }
}

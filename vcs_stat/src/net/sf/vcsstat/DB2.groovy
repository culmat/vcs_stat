package net.sf.vcsstat

import groovy.sql.Sql

import javax.sql.DataSource

import org.h2.jdbcx.JdbcConnectionPool

class DB2 {

	static DataSource source = JdbcConnectionPool.create("jdbc:h2:~/h2data.vcs_stat", "sa", "sa")
	static Sql db = new Sql(source)
	static {
		playFile("createTable.sql");
	}


	public static void main(String[] args) {
		db.eachRow("SELECT * from REVISION"){ println it }
		db.eachRow("SELECT YEAR, AUTHOR,  count(*) from REVISION group by AUTHOR , YEAR"){ println it }
	}


	public static Long count() {
		db.firstRow("SELECT count(*) from REVISION;")[0]
	}

	public static store(repo,path,ctime,year,week,added,removed,delta,author) {
		db.executeInsert("insert into REVISION (REPO,PATH,CTIME,YEAR,WEEK,ADDED,REMOVED,DELTA,AUTHOR) VALUES (?,?,?,?,?,?,?,?,?)",
				repo,path,ctime,year,week,added,removed,delta,author)
	}


	private static void playFile(String name) {
		long start = System.currentTimeMillis()
		print "Playing $name"
		db.execute DB2.class.getResourceAsStream(name).text
		println(" took "+(System.currentTimeMillis()-start))
	}

	public static read(repo, path, ctime) {
		return db.firstRow("select * from REVISION where REPO = ? and PATH = ? and ctime=?", repo, path, ctime)
	}
}

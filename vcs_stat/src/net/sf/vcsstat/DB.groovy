package net.sf.vcsstat
import groovy.sql.DataSet
import groovy.sql.Sql

import java.util.Calendar

import javax.sql.DataSource

import net.sf.vcsstat.graph.DataTable

import org.h2.jdbcx.JdbcConnectionPool
class DB {

	static DataSource source = JdbcConnectionPool.create("jdbc:h2:~/h2data.vcs_stat", "sa", "sa")
	static Sql db = new Sql(source)
	static DataSet revisions = db.dataSet 'REVISION'
	static {
		playFile("createTable.sql");
	}


	public static void main(String[] args) {

		//revisions.each { println it }
		//db.eachRow("SELECT * from REVISION where path1='ivd'  order by ctime "){ println it }


		//		db.eachRow("SELECT WEEK, AUTHOR, count(*) as COMMITS from REVISION group by AUTHOR , WEEK order by WEEK"){ println it }
		//		db.eachRow("SELECT WEEK, AUTHOR, SUM(DELTA) as DELTA from REVISION group by AUTHOR , WEEK order by WEEK"){ println it }

		//byExtension()

		//		db.executeUpdate("delete from REVISION where path = 'java/src/tmp.txt'")
		//db.eachRow("SELECT * from REVISION where path = 'java/src/tmp.txt'"){ println it }

		//		db.eachRow("SELECT path, SUM(DELTA) as SUMDELTA from REVISION group by path order by SUMDELTA"){ println it }

		//		db.eachRow("SELECT distinct repo from REVISION"){ println it }

		//		db.executeUpdate("delete from revision where path1='ivd'")

		//db.eachRow("SELECT count( distinct path)   as pathCount from REVISION where path1='ivd'"){ println it }

		///spu/test/cvs/admin
		
		println repos()
		
		//println totalLines()

	}

	private static byExtension() {
		//		List exts = db.rows("SELECT EXTENSION, SUM(DELTA) as DELTA from REVISION group by EXTENSION order by DELTA desc limit 6").collect{ it.EXTENSION }
		//		exts.each {
		//			println it
		//		}
		//		def extsList = exts.collect {"'$it'"}.join(',')
		DataTable data = new DataTable('Month')
		def currentExt
		int sum
		db.eachRow("SELECT EXTENSION, week, SUM(DELTA) as DELTA from REVISION group by EXTENSION, week order by EXTENSION, week"){
			if(it.EXTENSION != currentExt){
				currentExt = it.EXTENSION
				sum = 0
			}
			sum+=it.DELTA
			String x = it.WEEK
			x = x[0..3]+" "+x[4..5]
			data.set(currentExt, x, sum)
		}
		println data.compact('Other', 7).asGoogleChart()
	}





	public static Long locBefore(Date date, String repo, String path) {
		nullIsZero db.firstRow("select sum(DELTA) from revision where repo = ? and path=? and ctime < ?", repo, path, date)[0]
	}

	public static Long totalLines() {
		nullIsZero db.firstRow("select sum(DELTA) from revision")[0]
	}

	public static repos() {
		db.rows("select distinct repo from revision").collect {
			it.REPO
		}
	}

	public static Long nullIsZero(Long l){
		l ? l : 0
	}

	public static Date getLastDateForFile(String path, String repo) {
		db.firstRow("SELECT max(CTIME) from REVISION where path = ? and repo=?", path, repo)[0]
	}


	public static Date getLastEntry(String path, String repo) {
		if(!path || "." == path){
			return 	db.firstRow("SELECT max(CTIME) from REVISION where repo=?", repo)[0]
		}
		def paths = path.split('/')
		def exts = paths[paths.length-1].split('\\.')
		if(paths.length <= 1) {
			return 	db.firstRow("SELECT max(CTIME) from REVISION where path1 = ? and repo=?"
			, paths[0], repo)[0]
		} else if(paths.length == 2) {
			return db.firstRow("SELECT max(CTIME) from REVISION where path1 = ? and path2 = ? and repo=?"
			,paths[0]
			,paths[1], repo)[0]
		} else if(paths.length == 3) {
			return db.firstRow("SELECT max(CTIME) from REVISION where path1 = ? and path2 = ? and path3 = ? and repo=?"
			,paths[0]
			,paths[1]
			,paths[2], repo)[0]
		} else {
			return 	db.firstRow("SELECT max(CTIME) from REVISION where path1 = ? and path2 = ? and path3 = ? and path like ? and repo=?"
			,paths[0]
			,paths[1]
			,paths[2]
			,"%$path", repo)[0]
		}
	}

	public static Long count() {
		db.firstRow("SELECT count(*) from REVISION;")[0]
	}


	public static boolean exists(Date date, String repo, String path) {
		db.firstRow("SELECT count(*) from REVISION where path = ? and repo=? and ctime =?", path, repo, date)[0]
	}

	public static store(repo,path,Date ctime,added,removed,author) {
		Calendar cal = Calendar.instance
		cal.setTime(ctime)
		int year = cal.get Calendar.YEAR
		int week = year * 100 + cal.get (Calendar.WEEK_OF_YEAR)
		def paths = path.split('/')
		def exts = paths[paths.length-1].split('\\.')
		revisions.add(repo:repo
				,path:path
				,path1:paths.length >1 ? paths[0]:null
				,path2:paths.length >2 ? paths[1]:null
				,path3:paths.length >3 ? paths[2]:null
				,extension:exts[exts.length-1]
				,ctime:ctime
				,year:year
				,week:week
				,day: cal.get(Calendar.DAY_OF_WEEK)
				,hour: cal.get(Calendar.HOUR_OF_DAY)
				,added:added
				,removed:removed
				,delta:(added - removed)
				,author:author
				)
	}


	private static void playFile(String name) {
		long start = System.currentTimeMillis()
		print "Playing $name"
		db.execute DB.class.getResourceAsStream(name).text
		println(" took "+(System.currentTimeMillis()-start))
	}

	public static read(repo, path, ctime) {
		return db.firstRow("select * from REVISION where REPO = ? and PATH = ? and CTIME=?", repo, path, ctime)
	}
}
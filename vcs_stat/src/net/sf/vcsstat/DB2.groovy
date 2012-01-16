package net.sf.vcsstat

import groovy.sql.DataSet
import groovy.sql.Sql

import java.text.SimpleDateFormat

import javax.sql.DataSource

import org.h2.jdbcx.JdbcConnectionPool

class DB2 {

	static DataSource source = JdbcConnectionPool.create("jdbc:h2:~/h2data.vcs_stat", "sa", "sa")
	static Sql db = new Sql(source)
	static DataSet revisions = db.dataSet 'REVISION'
	static {
		playFile("createTable.sql");
	}


	public static void main(String[] args) {
		revisions.each { println it }
		//		db.eachRow("SELECT YEAR, AUTHOR, count(*) as SUM from REVISION group by AUTHOR , YEAR order by YEAR"){ println it }
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
		println getLastEntry('ccvs/README','/sources/cvs')
		String dates = "'"
		dates += df.format(getLastEntry('ccvs/README','/sources/cvs'))
		dates += "<" + df.format(new Date(java.lang.System.currentTimeMillis()+9999999))
		dates += "'"
		println dates
	}

	public static Date getLastEntry(String path, String repo) {
		def paths = path.split('/')
		def exts = paths[paths.length-1].split('\\.')
		if(paths.length <= 1) {
			return 	db.firstRow("SELECT max(CTIME) from REVISION where path = ? and repo=?"
			, path, repo)[0]
		} else if(paths.length == 2) {
			return db.firstRow("SELECT max(CTIME) from REVISION where path1 = ? and path = ? and repo=?"
			,paths[0]
			,path, repo)[0]
		} else if(paths.length == 3) {
			return db.firstRow("SELECT max(CTIME) from REVISION where path1 = ? and path2 = ? and path = ? and repo=?"
			,paths[0]
			,paths[1]
			,path, repo)[0]
		} else {
			return 	db.firstRow("SELECT max(CTIME) from REVISION where path1 = ? and path2 = ? and path3 = ? and path = ? and repo=?"
			,paths[0]
			,paths[1]
			,paths[2]
			,path, repo)[0]
		}
	}

	public static Long count() {
		db.firstRow("SELECT count(*) from REVISION;")[0]
	}

	public static store(repo,path,path1,path2,path3,extension,ctime,year,week,added,removed,delta,author) {
		revisions.add(repo:repo,path:path,path1:path1,
				path2:path2, path3:path3, extension:extension,
				ctime:ctime,year:year,week:week,added:added,removed:removed,delta:delta,author:author)
	}


	private static void playFile(String name) {
		long start = System.currentTimeMillis()
		print "Playing $name"
		db.execute DB2.class.getResourceAsStream(name).text
		println(" took "+(System.currentTimeMillis()-start))
	}

	public static read(repo, path, ctime) {
		return db.firstRow("select * from REVISION where REPO = ? and PATH = ? and CTIME=?", repo, path, ctime)
	}
}

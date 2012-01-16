package net.sf.vcsstat

import java.util.regex.Matcher
import java.util.regex.Pattern

class PathInfo {

	def PathInfo(String path){
		def ret = path.split('/')
		def ext = ret[ret.length-1].split('\\.')
	}
}

class Crawler {
	static Pattern FILE = Pattern.compile('RCS file: (.*?),v')
	static Pattern REVISION = Pattern.compile('date: (.*?);  author: (.*?);  state: .*?;(  lines: (.*?);)?')
	static Pattern LINES = Pattern.compile('\\+(\\d+) -(\\d+)')
	int totalDelta = 0
	String path
	static main(args) {
		Crawler c = new Crawler()
		//c.crawl()
		println c.totalDelta
		println pathInfo('ccvs/README').inspect()
	}

	String cvsCmd_ = 'cvs -z3 -Q -d:pserver:anonymous@cvs.savannah.nongnu.org:'
	String cvsContext = '/sources/cvs'
	String cvsCmd =  cvsCmd_ + cvsContext + ' '
	String rlog = 'rlog -b -N -S -l '
	String co = 'co -r 1.1 -p '

	def crawl(){

		String path = 'ccvs/README'
		println "Crawling $path"

		int i
		(cvsCmd + rlog + path).execute().inputStream.eachLine{ line ->
			Matcher m = FILE.matcher(line);
			if (m.find()) {
				recFile(m.group(1))
			} else {
				m = REVISION.matcher(line)
				if(m.find()) recRevision(m)
			}
		}
	}

	def recFile(String path){
		this.path = path - cvsContext - '/';
		println path
	}
	def recRevision(Matcher m){
		recRevision(m.group(1), m.group(2), m.group(4))
	}
	def recRevision(String dateString, String author, String lines){
		if(! lines ) lines = countInitialRevision();
		def m = LINES.matcher lines
		m.find()
		int added = Integer.valueOf(m.group(1))
		int removed = Integer.valueOf(m.group(2))
		int delta = added - removed
		totalDelta += delta
		Date date = Date.parse('yyyy-MM-dd HH:mm:ss Z', dateString)
		println "$date -- $author -- $lines -- $delta"
		Calendar cal = Calendar.getInstance()
		cal.setTime(date)
		int year = cal.get Calendar.YEAR
		int week = year * 100 + cal.get (Calendar.WEEK_OF_YEAR)
		try {
			DB2.store(cvsContext,path,date,year,week,added,removed,delta,author)
		} catch (Exception e){
			println "Error storing $cvsContext $path $date"
		}
	}
	String  countInitialRevision(){
		String cmd = cvsCmd + co + path
		int lc
		cmd.execute().inputStream.eachLine{ lc++}
		return "+$lc -0"
	}
}

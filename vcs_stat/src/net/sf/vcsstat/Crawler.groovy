package net.sf.vcsstat

import java.util.regex.Matcher
import java.util.regex.Pattern


class Crawler {
	static Pattern FILE = Pattern.compile('RCS file: (.*?),v')
	static Pattern REVISION = Pattern.compile('date: (.*?);  author: (.*?);  state: .*?;(  lines: (.*?);)?')
	static Pattern LINES = Pattern.compile('\\+(\\d+) -(\\d+)')
	int totalDelta = 0
	String path
	static main(args) {
		Crawler c = new Crawler()
		c.crawl('ccvs/README')
		println c.totalDelta
	}

	String cvsCmd_ = 'cvs -z3 -Q -d:pserver:anonymous@cvs.savannah.nongnu.org:'
	String cvsContext = '/sources/cvs'
	String cvsCmd =  cvsCmd_ + cvsContext + ' '
	String rlog = 'rlog -b -N -S -l '
	String co = 'co -r 1.1 -p '

	static String trimSlash(String s){
		String ret = s
		if(ret.startsWith('/')) ret = ret.substring(1)
		if(ret.endsWith('/')) ret = ret.substring(0, ret.length()-1)
		return ret
	}


	def crawl(String startPath){
		startPath = trimSlash(startPath)
		println "Crawling $startPath"

		int i
		def cvsProcess = (cvsCmd + rlog + startPath).execute()
		Thread.start {
			cvsProcess.errorStream.eachLine { System.err.println it }
		}
		cvsProcess.inputStream.eachLine{ line ->
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
		this.path = trimSlash(path - cvsContext - '/')
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
		int delta = (added - removed);
		totalDelta += delta
		Date date = Date.parse('yyyy-MM-dd HH:mm:ss Z', dateString)
		println "$date -- $author -- $lines -- $delta"
		try {
			DB2.store(cvsContext,
					path,
					date,
					added,
					removed,
					author)
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

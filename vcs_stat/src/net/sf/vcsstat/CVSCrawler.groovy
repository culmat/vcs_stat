package net.sf.vcsstat

import java.text.SimpleDateFormat
import java.util.regex.Matcher
import java.util.regex.Pattern



class CVSCrawler {
	static Pattern FILE = Pattern.compile('RCS file: (.*?),v')
	static Pattern REVISION = Pattern.compile('date: (.*?);  author: (.*?);  state: (.*?);(  lines: (\\+\\d+ -\\d+))?')
	static Pattern LINES = Pattern.compile('\\+(\\d+) -(\\d+)')
	String path
	final String cvsContext
	final String mount
	static def cvsCmd =  ['cvs', '-Q']
	//final  def  co = ['co' , '-r' , '1.1', '-p']
	final  def  rlog = ['rlog', '-b', '-N', '-S']

	public CVSCrawler(String cvsroot, String repo, String startPath, String mnt){
		rlog = rlog + getDateFrom(startPath,repo)
		cvsCmd =  cvsCmd + "-d:$cvsroot:$repo"
		cvsContext = repo
		mount = mnt
	}
	static main(args) {
		if(args.length < 4) {
			println('usage: CVSCrawler <CVSROOT> <repo> <path> <mount>')
			return
		}
		new CVSCrawler(args[0],args[1],args[2],args[3] ).crawl(args[2])
	}


	private static getDateFrom(String path, String repo) {
		def last = DB.getLastEntry(path, repo)
		return last ? [
			'-d',
			df2.format(last)+'<'
		]
		: []
	}

	static String trimSlash(String s){
		String ret = s
		if(ret.startsWith('/')) ret = ret.substring(1)
		if(ret.endsWith('/')) ret = ret.substring(0, ret.length()-1)
		return ret
	}

	long counter
	def crawl(String startPath){
		long startTime = System.currentTimeMillis()
		startPath = trimSlash(startPath)
		println "Crawling $startPath at ${df2.format(new Date())}"
		boolean running = true
		Thread.startDaemon {
			while(running) {
				sleep 5000
				def durationSec = (System.currentTimeMillis()-startTime)/1000
				def filesPerSec = counter / durationSec
				println "Curent location: $cvsContext $path"
				println "Files: $counter \t Throughput $filesPerSec files/sec"
			}
		}
		println ((cvsCmd + rlog + startPath).inspect())
		def cvsProcess = (cvsCmd + rlog + startPath).execute()
		Thread errdump = Thread.startDaemon {
			System.err << cvsProcess.err
		}
		cvsProcess.in.eachLine{ line ->
			//println line
			Matcher m = FILE.matcher line
			if (m.find()) {
				recFile(m.group (1) )
			} else {
				m = REVISION.matcher line
				if(m.find()) recRevision m
			}
		}
		running = false
		errdump.stop()
		storeOps()
		def duration = Format.time (System.currentTimeMillis()-startTime)
		println "Finished at ${df2.format(new Date())} after $duration"
	}


	def recFile(String path){
		storeOps()
		this.path = trimSlash(path - cvsContext - '/')
		//println path
		counter++
	}
	List ops = []
	def recRevision(Matcher m){
		if('dead' == m.group(3)?.trim()){
			ops << [date: parseDate(m.group(1)), author:m.group(2), isDead:true]
		} else {
			recRevision(m.group(1), m.group(2), m.group(5))
		}
	}

	def storeOps(){
		ops.reverse().each {
			if(it.isDead)
				store(it.date, it.author, 0, (int)DB.locBefore(it.date, cvsContext, path))
			else
				store(it.date, it.author, it.added, checkNotRemoved(it.removed, it.date))
		}
		ops = []
	}

	def checkNotRemoved(int removed, Date date){
		if (removed > 0) {
			return (DB.locBefore(date, cvsContext, path) <=0) ? 0 : removed
		} else {
			return 0
		}
	}

	boolean updated(Date date){
		Date last = DB.getLastDateForFile(path, cvsContext)
		return !last || date.after(last)
	}

	def recRevision(String dateString, String author, String lines){
		Date date = parseDate(dateString)
		if(! lines) {
			if(!DB.exists(date, cvsContext,path))
				lines = countInitialRevision()
			else
				return
		}
		def m = LINES.matcher lines
		m.find()
		int added = Integer.valueOf(m.group(1))
		int removed = Integer.valueOf(m.group(2))
		ops << [date: date, author:author, added :added, removed:removed]
	}

	private store(Date date, String author, int added, int removed) {
		//println "$path -- $date -- $author "
		try {
			DB.store(cvsContext,
					path,
					date,
					added,
					removed,
					author)
		} catch (Exception e){
			println "Error storing $cvsContext $path $date"
		}
	}

	static SimpleDateFormat df1 = new SimpleDateFormat('yyyy-MM-dd HH:mm:ss Z')
	static SimpleDateFormat df2 = new SimpleDateFormat('yyyy/MM/dd HH:mm:ss')
	static Date parseDate(String dateString){
		try {
			return df1.parse(dateString)
		} catch (Exception e) {
			return df2.parse(dateString)
		}
	}

	String  countInitialRevision(){
		int lc
		//def cvsProcess = (cvsCmd + co + path).execute()
		def cvsProcess = (['co', '-q', '-p']+ (mount+cvsContext+'/'+path+',v')).execute()
		Thread errdump = Thread.startDaemon {
			System.err << cvsProcess.err
		}
		cvsProcess.in.eachLine { lc++ }
		cvsProcess.err.close()
		cvsProcess.in.close()
		errdump.stop()
		return "+$lc -0"
	}
}

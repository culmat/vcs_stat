--DROP TABLE REVISION;
CREATE  TABLE IF NOT EXISTS REVISION (
  REPO VARCHAR NOT NULL,
  PATH VARCHAR NOT NULL,
  PATH1 VARCHAR,
  PATH2 VARCHAR,
  PATH3 VARCHAR,
  EXTENSION VARCHAR,
  CTIME TIMESTAMP NOT NULL,
  YEAR INT,
  WEEK INT,
  DAY INT,
  HOUR INT,
  ADDED INT,
  REMOVED INT,
  DELTA INT,
  AUTHOR VARCHAR,
  PRIMARY KEY (REPO, PATH, CTIME)
  );
  
CREATE INDEX IF NOT EXISTS REPO ON REVISION (REPO);
CREATE INDEX IF NOT EXISTS PATH ON REVISION (PATH);
CREATE INDEX IF NOT EXISTS PATH1 ON REVISION (PATH1);
CREATE INDEX IF NOT EXISTS PATH2 ON REVISION (PATH2);
CREATE INDEX IF NOT EXISTS PATH3 ON REVISION (PATH3);
CREATE INDEX IF NOT EXISTS EXTENSION ON REVISION (EXTENSION);
CREATE INDEX IF NOT EXISTS CTIME ON REVISION (CTIME);
CREATE INDEX IF NOT EXISTS YEAR ON REVISION (YEAR);
CREATE INDEX IF NOT EXISTS WEEK ON REVISION (WEEK);
CREATE INDEX IF NOT EXISTS AUTHOR ON REVISION (AUTHOR);

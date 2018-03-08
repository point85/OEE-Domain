-- SQL Server script file
-- set to your database name
USE [JPA]
GO

SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

SET ANSI_PADDING ON
GO

/****** sequence generator ******/
IF OBJECT_ID('dbo.JPA_SEQ', 'SO') IS NOT NULL 
    DROP SEQUENCE dbo.JPA_SEQ
GO

CREATE SEQUENCE [dbo].[JPA_SEQ] 
 AS [bigint]
 START WITH 1
 INCREMENT BY 10
 MINVALUE 1
 MAXVALUE 9223372036854775807
 CACHE 
GO

/****** Plant Entity table ******/
IF OBJECT_ID('dbo.PLANT_ENTITY', 'U') IS NOT NULL 
  DROP TABLE dbo.PLANT_ENTITY
GO

CREATE TABLE [dbo].[PLANT_ENTITY](
	[ENT_KEY] [bigint] NULL,
	[VERSION] [int] NULL,
	[NAME] [nvarchar](64) NULL,
	[DESCRIPTION] [nvarchar](128) NULL,
	[PARENT_KEY] [bigint] NULL,
	[LEVEL] [nvarchar](16) NULL,
	[WS_KEY] [bigint] NULL,
	[IS_ROOT] [bit] NULL
) ON [PRIMARY]
GO
CREATE UNIQUE NONCLUSTERED INDEX [IDX_NAME] ON [dbo].[PLANT_ENTITY]
(
	[NAME] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO

/****** Reason table ******/
IF OBJECT_ID('dbo.REASON', 'U') IS NOT NULL 
  DROP TABLE dbo.REASON
GO

CREATE TABLE [dbo].[REASON](
	[REASON_KEY] [bigint] NULL,
	[VERSION] [int] NULL,
	[NAME] [nvarchar](64) NULL,
	[DESCRIPTION] [nvarchar](128) NULL,
	[ENT_KEY] [bigint] NULL,
	[PARENT_KEY] [bigint] NULL,
	[LOSS] [nvarchar](32) NULL
) ON [PRIMARY]
GO
CREATE UNIQUE NONCLUSTERED INDEX [IDX_NAME] ON [dbo].[REASON]
(
	[NAME] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO

/****** Material table ******/
IF OBJECT_ID('dbo.MATERIAL', 'U') IS NOT NULL 
  DROP TABLE dbo.MATERIAL
GO

CREATE TABLE [dbo].[MATERIAL](
	[MAT_KEY] [bigint] NULL,
	[VERSION] [int] NULL,
	[ID] [nvarchar](64) NULL,
	[DESCRIPTION] [nvarchar](128) NULL,
	[CATEGORY] [nvarchar](32) NULL
) ON [PRIMARY]
GO
CREATE UNIQUE NONCLUSTERED INDEX [IDX_NAME] ON [dbo].[MATERIAL]
(
	[ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO

/****** Equipment Material table ******/
IF OBJECT_ID('dbo.EQUIPMENT_MATERIAL', 'U') IS NOT NULL 
  DROP TABLE dbo.EQUIPMENT_MATERIAL
GO

CREATE TABLE [dbo].[EQUIPMENT_MATERIAL](
	[EM_KEY] [bigint] NULL,
	[MAT_KEY] [bigint] NULL,
	[EQ_KEY] [bigint] NULL,
	[OEE_TARGET] [float] NULL,
	[RUN_AMOUNT] [float] NULL,
	[RUN_UOM_KEY] [bigint] NULL,
	[REJECT_UOM_KEY] [bigint] NULL
) ON [PRIMARY]
GO

/****** Data Source table ******/
IF OBJECT_ID('dbo.DATA_SOURCE', 'U') IS NOT NULL 
  DROP TABLE dbo.DATA_SOURCE
GO

CREATE TABLE [dbo].[DATA_SOURCE](
	[SOURCE_KEY] [bigint] NOT NULL,
	[VERSION] [int] NOT NULL,
	[NAME] [nvarchar](64) NOT NULL,
	[DESCRIPTION] [nvarchar](128) NULL,
	[TYPE] [nvarchar](16) NOT NULL,
	[HOST] [nvarchar](64) NULL,
	[USER_NAME] [nvarchar](64) NULL,
	[PASSWORD] [nvarchar](32) NULL,
	[PORT] [int] NULL,
	[PARAM1] [nvarchar](128) NULL
) ON [PRIMARY]
GO

CREATE UNIQUE NONCLUSTERED INDEX [IDX_NAME] ON [dbo].[DATA_SOURCE]
(
	[NAME] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO

/****** Script Resolver table ******/
IF OBJECT_ID('dbo.SCRIPT_RESOLVER', 'U') IS NOT NULL 
  DROP TABLE dbo.SCRIPT_RESOLVER
GO

CREATE TABLE [dbo].[SCRIPT_RESOLVER](
	[SR_KEY] [bigint] NULL,
	[ENT_KEY] [bigint] NULL,
	[SOURCE_ID] [nvarchar](128) NULL,
	[SCRIPT] [nvarchar](1024) NULL,
	[SOURCE_KEY] [bigint] NULL,
	[PERIOD] [int] NULL,
	[SR_TYPE] [nvarchar](16) NULL,
	[DATA_TYPE] [nvarchar](32) NULL,
	[COLLECT_KEY] [bigint] NULL
) ON [PRIMARY]
GO

/****** Unit of Measure table ******/
IF OBJECT_ID('dbo.UOM', 'U') IS NOT NULL 
  DROP TABLE dbo.UOM; 
GO

CREATE TABLE [dbo].[UOM](
	[UOM_KEY] [bigint] NOT NULL,
	[VERSION] [int] NOT NULL,
	[NAME] [nvarchar](50) NULL,
	[SYMBOL] [nvarchar](16) NOT NULL,
	[DESCRIPTION] [nvarchar](512) NULL,
	[CATEGORY] [nvarchar](50) NULL,
	[UNIT_TYPE] [nvarchar](32) NULL,
	[UNIT] [nvarchar](32) NULL,
	[CONV_FACTOR] [float] NULL,
	[CONV_UOM_KEY] [bigint] NULL,
	[CONV_OFFSET] [float] NULL,
	[BRIDGE_FACTOR] [float] NULL,
	[BRIDGE_UOM_KEY] [bigint] NULL,
	[BRIDGE_OFFSET] [float] NULL,
	[UOM1_KEY] [bigint] NULL,
	[EXP1] [int] NULL,
	[UOM2_KEY] [bigint] NULL,
	[EXP2] [int] NULL,
 CONSTRAINT [PK_UOM] PRIMARY KEY CLUSTERED 
(
	[UOM_KEY] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [IX_SYMBOL] UNIQUE NONCLUSTERED 
(
	[SYMBOL] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO

/****** WORK_SCHEDULE table ******/
IF OBJECT_ID('dbo.WORK_SCHEDULE', 'U') IS NOT NULL 
  DROP TABLE dbo.WORK_SCHEDULE; 
GO

CREATE TABLE [dbo].[WORK_SCHEDULE](
	[WS_KEY] [bigint] NOT NULL,
	[NAME] [nvarchar](64) NULL,
	[DESCRIPTION] [nvarchar](512) NULL,
	[VERSION] [int] NULL
) ON [PRIMARY]
GO
CREATE UNIQUE NONCLUSTERED INDEX [IX_NAME] ON [dbo].[WORK_SCHEDULE]
(
	[NAME] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO

/****** SHIFT table ******/
IF OBJECT_ID('dbo.SHIFT', 'U') IS NOT NULL 
  DROP TABLE dbo.SHIFT; 
GO

CREATE TABLE [dbo].[SHIFT](
	[SHIFT_KEY] [bigint] NOT NULL,
	[NAME] [nvarchar](64) NULL,
	[DESCRIPTION] [nvarchar](128) NULL,
	[START_TIME] [time](7) NULL,
	[DURATION] [bigint] NULL,
	[WS_KEY] [int] NULL
) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [IX_NAME] ON [dbo].[SHIFT]
(
	[NAME] ASC,
	[WS_KEY] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO

/****** TEAM table ******/
IF OBJECT_ID('dbo.TEAM', 'U') IS NOT NULL 
  DROP TABLE dbo.TEAM; 
GO

CREATE TABLE [dbo].[TEAM](
	[TEAM_KEY] [bigint] NOT NULL,
	[NAME] [nvarchar](64) NULL,
	[DESCRIPTION] [nvarchar](128) NULL,
	[WS_KEY] [int] NULL,
	[ROTATION_KEY] [int] NULL,
	[ROTATION_START] [date] NULL
) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [IX_NAME] ON [dbo].[TEAM]
(
	[NAME] ASC,
	[WS_KEY] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO

/****** ROTATION table ******/
IF OBJECT_ID('dbo.ROTATION', 'U') IS NOT NULL 
  DROP TABLE dbo.ROTATION; 
GO

CREATE TABLE [dbo].[ROTATION](
	[ROTATION_KEY] [bigint] NOT NULL,
	[NAME] [nvarchar](64) NULL,
	[DESCRIPTION] [nvarchar](128) NULL
) ON [PRIMARY]
GO

/****** ROTATION SEGMENT table ******/
IF OBJECT_ID('dbo.ROTATION_SEGMENT', 'U') IS NOT NULL 
  DROP TABLE dbo.ROTATION_SEGMENT; 
GO

CREATE TABLE [dbo].[ROTATION_SEGMENT](
	[SEGMENT_KEY] [bigint] NOT NULL,
	[ROTATION_KEY] [int] NULL,
	[SEQUENCE] [smallint] NULL,
	[SHIFT_KEY] [smallint] NULL,
	[DAYS_ON] [smallint] NULL,
	[DAYS_OFF] [smallint] NULL
) ON [PRIMARY]
GO

/****** NON-WORKING PERIOD table ******/
IF OBJECT_ID('dbo.NON_WORKING_PERIOD', 'U') IS NOT NULL 
  DROP TABLE dbo.NON_WORKING_PERIOD; 
GO

CREATE TABLE [dbo].[NON_WORKING_PERIOD](
	[PERIOD_KEY] [bigint] NOT NULL,
	[NAME] [nvarchar](64) NULL,
	[DESCRIPTION] [nvarchar](128) NULL,
	[START_DATE_TIME] [datetime] NULL,
	[DURATION] [bigint] NULL,
	[WS_KEY] [int] NULL,
	[LOSS] [nvarchar](32) NULL
) ON [PRIMARY]
GO

/****** COLLECTOR table ******/
IF OBJECT_ID('dbo.COLLECTOR', 'U') IS NOT NULL 
  DROP TABLE dbo.COLLECTOR 
GO

CREATE TABLE [dbo].[COLLECTOR](
	[COLLECT_KEY] [bigint] NOT NULL,
	[VERSION] [int] NULL,
	[NAME] [nvarchar](64) NULL,
	[DESCRIPTION] [nvarchar](128) NULL,
	[STATE] [nvarchar](16) NULL,
	[HOST] [nvarchar](64) NULL,
	[BROKER_HOST] [nvarchar](64) NULL,
	[BROKER_PORT] [int] NULL
) ON [PRIMARY]
GO
CREATE UNIQUE NONCLUSTERED INDEX [IDX_NAME] ON [dbo].[COLLECTOR]
(
	[NAME] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO

/****** EVENT_HISTORY table ******/
IF OBJECT_ID('dbo.EVENT_HISTORY', 'U') IS NOT NULL 
  DROP TABLE dbo.EVENT_HISTORY 
GO

CREATE TABLE [dbo].[EVENT_HISTORY](
	[EVENT_KEY] [bigint] IDENTITY(1,1) NOT NULL,
	[ENT_KEY] [bigint] NOT NULL,
	[MATL_KEY] [bigint] NULL,
	[JOB] [nvarchar](64) NULL,
	[EVENT_TIME] [datetimeoffset](3) NULL,
	[REASON_KEY] [bigint] NULL,
	[TYPE] [nchar](16) NULL
) ON [PRIMARY]
GO

/****** PROD_HISTORY table ******/
IF OBJECT_ID('dbo.PROD_HISTORY', 'U') IS NOT NULL 
  DROP TABLE dbo.PROD_HISTORY
GO

CREATE TABLE [dbo].[PROD_HISTORY](
	[EVENT_KEY] [bigint] IDENTITY(1,1) NOT NULL,
	[ENT_KEY] [bigint] NULL,
	[MATL_KEY] [bigint] NULL,
	[JOB] [nvarchar](64) NULL,
	[EVENT_TIME] [datetimeoffset](3) NULL,
	[TYPE] [nvarchar](16) NULL,
	[AMOUNT] [float] NULL,
	[UOM_KEY] [bigint] NULL
) ON [PRIMARY]
GO

/****** SETUP_HISTORY table ******/
IF OBJECT_ID('dbo.SETUP_HISTORY', 'U') IS NOT NULL 
  DROP TABLE dbo.SETUP_HISTORY
GO

CREATE TABLE [dbo].[SETUP_HISTORY](
	[EVENT_KEY] [bigint] IDENTITY(1,1) NOT NULL,
	[ENT_KEY] [bigint] NULL,
	[MATL_KEY] [bigint] NULL,
	[JOB] [nvarchar](64) NULL,
	[EVENT_TIME] [datetimeoffset](3) NULL,
	[TYPE] [nchar](16) NULL
) ON [PRIMARY]
GO





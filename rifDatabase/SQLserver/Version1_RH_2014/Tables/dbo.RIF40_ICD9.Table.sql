USE [RIF40]
GO
/****** Object:  Table [dbo].[RIF40_ICD9]    Script Date: 19/09/2014 12:07:53 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[RIF40_ICD9](
	[ICD9_3CHAR] [varchar](3) NULL,
	[ICD9_4CHAR] [varchar](4) NOT NULL,
	[TEXT_3CHAR] [varchar](250) NULL,
	[TEXT_4CHAR] [varchar](250) NULL,
 CONSTRAINT [RIF40_ICD9_PK] PRIMARY KEY CLUSTERED 
(
	[ICD9_4CHAR] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.RIF40_ICD9.ICD9_3CHAR' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'RIF40_ICD9', @level2type=N'COLUMN',@level2name=N'ICD9_3CHAR'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.RIF40_ICD9.ICD9_4CHAR' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'RIF40_ICD9', @level2type=N'COLUMN',@level2name=N'ICD9_4CHAR'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.RIF40_ICD9.TEXT_3CHAR' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'RIF40_ICD9', @level2type=N'COLUMN',@level2name=N'TEXT_3CHAR'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.RIF40_ICD9.TEXT_4CHAR' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'RIF40_ICD9', @level2type=N'COLUMN',@level2name=N'TEXT_4CHAR'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.RIF40_ICD9' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'RIF40_ICD9'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.RIF40_ICD9.RIF40_ICD9_PK' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'RIF40_ICD9', @level2type=N'CONSTRAINT',@level2name=N'RIF40_ICD9_PK'
GO

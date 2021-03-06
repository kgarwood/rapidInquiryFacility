USE [RIF40]
GO
/****** Object:  Table [dbo].[RIF40_ICD_O_3]    Script Date: 19/09/2014 12:07:53 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[RIF40_ICD_O_3](
	[ICD_O_3_1CHAR] [varchar](20) NULL,
	[ICD_O_3_4CHAR] [varchar](4) NOT NULL,
	[TEXT_1CHAR] [varchar](250) NULL,
	[TEXT_4CHAR] [varchar](250) NULL,
 CONSTRAINT [RIF40_ICD_O_3_PK] PRIMARY KEY CLUSTERED 
(
	[ICD_O_3_4CHAR] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.RIF40_ICD_O_3.ICD_O_3_1CHAR' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'RIF40_ICD_O_3', @level2type=N'COLUMN',@level2name=N'ICD_O_3_1CHAR'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.RIF40_ICD_O_3.ICD_O_3_4CHAR' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'RIF40_ICD_O_3', @level2type=N'COLUMN',@level2name=N'ICD_O_3_4CHAR'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.RIF40_ICD_O_3.TEXT_1CHAR' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'RIF40_ICD_O_3', @level2type=N'COLUMN',@level2name=N'TEXT_1CHAR'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.RIF40_ICD_O_3.TEXT_4CHAR' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'RIF40_ICD_O_3', @level2type=N'COLUMN',@level2name=N'TEXT_4CHAR'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.RIF40_ICD_O_3' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'RIF40_ICD_O_3'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.RIF40_ICD_O_3.RIF40_ICD_O_3_PK' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'RIF40_ICD_O_3', @level2type=N'CONSTRAINT',@level2name=N'RIF40_ICD_O_3_PK'
GO

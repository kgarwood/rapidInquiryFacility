USE [RIF40]
GO
/****** Object:  Table [dbo].[T_RIF40_INV_COVARIATES]    Script Date: 19/09/2014 12:07:53 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[T_RIF40_INV_COVARIATES](
	[INV_ID] [numeric](8, 0) NOT NULL,
	[STUDY_ID] [numeric](8, 0) NOT NULL,
	[COVARIATE_NAME] [varchar](20) NOT NULL,
	[USERNAME] [varchar](90) NOT NULL,
	[GEOGRAPHY] [varchar](30) NOT NULL,
	[STUDY_GEOLEVEL_NAME] [varchar](30) NULL,
	[MIN] [numeric](9, 3) NOT NULL,
	[MAX] [numeric](9, 3) NOT NULL,
 CONSTRAINT [T_RIF40_INV_COVARIATES_PK] PRIMARY KEY CLUSTERED 
(
	[STUDY_ID] ASC,
	[INV_ID] ASC,
	[COVARIATE_NAME] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
ALTER TABLE [dbo].[T_RIF40_INV_COVARIATES] ADD  DEFAULT (user_name()) FOR [USERNAME]
GO
ALTER TABLE [dbo].[T_RIF40_INV_COVARIATES]  WITH NOCHECK ADD  CONSTRAINT [T_RIF40_INV_COVARIATES_SI_FK] FOREIGN KEY([STUDY_ID], [INV_ID])
REFERENCES [dbo].[T_RIF40_INVESTIGATIONS] ([STUDY_ID], [INV_ID])
GO
ALTER TABLE [dbo].[T_RIF40_INV_COVARIATES] CHECK CONSTRAINT [T_RIF40_INV_COVARIATES_SI_FK]
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_INV_COVARIATES.INV_ID' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_INV_COVARIATES', @level2type=N'COLUMN',@level2name=N'INV_ID'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_INV_COVARIATES.STUDY_ID' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_INV_COVARIATES', @level2type=N'COLUMN',@level2name=N'STUDY_ID'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_INV_COVARIATES.COVARIATE_NAME' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_INV_COVARIATES', @level2type=N'COLUMN',@level2name=N'COVARIATE_NAME'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_INV_COVARIATES.USERNAME' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_INV_COVARIATES', @level2type=N'COLUMN',@level2name=N'USERNAME'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_INV_COVARIATES.GEOGRAPHY' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_INV_COVARIATES', @level2type=N'COLUMN',@level2name=N'GEOGRAPHY'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_INV_COVARIATES.STUDY_GEOLEVEL_NAME' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_INV_COVARIATES', @level2type=N'COLUMN',@level2name=N'STUDY_GEOLEVEL_NAME'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_INV_COVARIATES.MIN' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_INV_COVARIATES', @level2type=N'COLUMN',@level2name=N'MIN'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_INV_COVARIATES.MAX' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_INV_COVARIATES', @level2type=N'COLUMN',@level2name=N'MAX'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_INV_COVARIATES' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_INV_COVARIATES'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_INV_COVARIATES.T_RIF40_INV_COVARIATES_PK' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_INV_COVARIATES', @level2type=N'CONSTRAINT',@level2name=N'T_RIF40_INV_COVARIATES_PK'
GO

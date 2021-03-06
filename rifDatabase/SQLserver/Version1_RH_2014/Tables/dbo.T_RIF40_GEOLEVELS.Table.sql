USE [RIF40]
GO
/****** Object:  Table [dbo].[T_RIF40_GEOLEVELS]    Script Date: 19/09/2014 12:07:53 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[T_RIF40_GEOLEVELS](
	[GEOGRAPHY] [varchar](50) NOT NULL,
	[GEOLEVEL_NAME] [varchar](30) NOT NULL,
	[GEOLEVEL_ID] [numeric](2, 0) NOT NULL,
	[DESCRIPTION] [varchar](250) NOT NULL,
	[LOOKUP_TABLE] [varchar](30) NOT NULL,
	[LOOKUP_DESC_COLUMN] [varchar](30) NOT NULL,
	[CENTROIDXCOORDINATE_COLUMN] [varchar](30) NULL,
	[CENTROIDYCOORDINATE_COLUMN] [varchar](30) NULL,
	[SHAPEFILE] [varchar](512) NULL,
	[CENTROIDSFILE] [varchar](512) NULL,
	[SHAPEFILE_TABLE] [varchar](30) NULL,
	[SHAPEFILE_AREA_ID_COLUMN] [varchar](30) NULL,
	[SHAPEFILE_DESC_COLUMN] [varchar](30) NULL,
	[ST_SIMPLIFY_TOLERANCE] [numeric](6, 0) NULL,
	[CENTROIDS_TABLE] [varchar](30) NULL,
	[CENTROIDS_AREA_ID_COLUMN] [varchar](30) NULL,
	[AVG_NPOINTS_GEOM] [numeric](12, 0) NULL,
	[AVG_NPOINTS_OPT] [numeric](12, 0) NULL,
	[FILE_GEOJSON_LEN] [numeric](12, 0) NULL,
	[LEG_GEOM] [numeric](12, 1) NULL,
	[LEG_OPT] [numeric](12, 1) NULL,
	[COVARIATE_TABLE] [varchar](30) NULL,
	[RESTRICTED] [numeric](1, 0) NOT NULL,
	[RESOLUTION] [numeric](1, 0) NOT NULL,
	[COMPAREA] [numeric](1, 0) NOT NULL,
	[LISTING] [numeric](1, 0) NOT NULL,
	[id] [int] IDENTITY(1,1) NOT NULL,
 CONSTRAINT [T_RIF40_GEOLEVELS_PK] PRIMARY KEY CLUSTERED 
(
	[GEOGRAPHY] ASC,
	[GEOLEVEL_NAME] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
ALTER TABLE [dbo].[T_RIF40_GEOLEVELS] ADD  DEFAULT ((0)) FOR [RESTRICTED]
GO
ALTER TABLE [dbo].[T_RIF40_GEOLEVELS]  WITH NOCHECK ADD  CONSTRAINT [T_RIF40_GEOLEVELS_GEOG_FK] FOREIGN KEY([GEOGRAPHY])
REFERENCES [dbo].[RIF40_GEOGRAPHIES] ([GEOGRAPHY])
GO
ALTER TABLE [dbo].[T_RIF40_GEOLEVELS] CHECK CONSTRAINT [T_RIF40_GEOLEVELS_GEOG_FK]
GO
ALTER TABLE [dbo].[T_RIF40_GEOLEVELS]  WITH NOCHECK ADD  CONSTRAINT [T_RIF40_GEOL_COMPAREA_CK] CHECK  (([COMPAREA]=(1) OR [COMPAREA]=(0)))
GO
ALTER TABLE [dbo].[T_RIF40_GEOLEVELS] CHECK CONSTRAINT [T_RIF40_GEOL_COMPAREA_CK]
GO
ALTER TABLE [dbo].[T_RIF40_GEOLEVELS]  WITH NOCHECK ADD  CONSTRAINT [T_RIF40_GEOL_LISTING_CK] CHECK  (([LISTING]=(1) OR [LISTING]=(0)))
GO
ALTER TABLE [dbo].[T_RIF40_GEOLEVELS] CHECK CONSTRAINT [T_RIF40_GEOL_LISTING_CK]
GO
ALTER TABLE [dbo].[T_RIF40_GEOLEVELS]  WITH NOCHECK ADD  CONSTRAINT [T_RIF40_GEOL_RESOLUTION_CK] CHECK  (([RESOLUTION]=(1) OR [RESOLUTION]=(0)))
GO
ALTER TABLE [dbo].[T_RIF40_GEOLEVELS] CHECK CONSTRAINT [T_RIF40_GEOL_RESOLUTION_CK]
GO
ALTER TABLE [dbo].[T_RIF40_GEOLEVELS]  WITH NOCHECK ADD  CONSTRAINT [T_RIF40_GEOL_RESTRICTED_CK] CHECK  (([RESTRICTED]=(1) OR [RESTRICTED]=(0)))
GO
ALTER TABLE [dbo].[T_RIF40_GEOLEVELS] CHECK CONSTRAINT [T_RIF40_GEOL_RESTRICTED_CK]
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_GEOLEVELS.GEOGRAPHY' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_GEOLEVELS', @level2type=N'COLUMN',@level2name=N'GEOGRAPHY'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_GEOLEVELS.GEOLEVEL_NAME' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_GEOLEVELS', @level2type=N'COLUMN',@level2name=N'GEOLEVEL_NAME'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_GEOLEVELS.GEOLEVEL_ID' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_GEOLEVELS', @level2type=N'COLUMN',@level2name=N'GEOLEVEL_ID'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_GEOLEVELS.DESCRIPTION' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_GEOLEVELS', @level2type=N'COLUMN',@level2name=N'DESCRIPTION'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_GEOLEVELS.LOOKUP_TABLE' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_GEOLEVELS', @level2type=N'COLUMN',@level2name=N'LOOKUP_TABLE'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_GEOLEVELS.LOOKUP_DESC_COLUMN' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_GEOLEVELS', @level2type=N'COLUMN',@level2name=N'LOOKUP_DESC_COLUMN'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_GEOLEVELS.CENTROIDXCOORDINATE_COLUMN' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_GEOLEVELS', @level2type=N'COLUMN',@level2name=N'CENTROIDXCOORDINATE_COLUMN'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_GEOLEVELS.CENTROIDYCOORDINATE_COLUMN' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_GEOLEVELS', @level2type=N'COLUMN',@level2name=N'CENTROIDYCOORDINATE_COLUMN'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_GEOLEVELS.SHAPEFILE' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_GEOLEVELS', @level2type=N'COLUMN',@level2name=N'SHAPEFILE'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_GEOLEVELS.CENTROIDSFILE' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_GEOLEVELS', @level2type=N'COLUMN',@level2name=N'CENTROIDSFILE'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_GEOLEVELS.SHAPEFILE_TABLE' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_GEOLEVELS', @level2type=N'COLUMN',@level2name=N'SHAPEFILE_TABLE'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_GEOLEVELS.SHAPEFILE_AREA_ID_COLUMN' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_GEOLEVELS', @level2type=N'COLUMN',@level2name=N'SHAPEFILE_AREA_ID_COLUMN'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_GEOLEVELS.SHAPEFILE_DESC_COLUMN' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_GEOLEVELS', @level2type=N'COLUMN',@level2name=N'SHAPEFILE_DESC_COLUMN'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_GEOLEVELS.ST_SIMPLIFY_TOLERANCE' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_GEOLEVELS', @level2type=N'COLUMN',@level2name=N'ST_SIMPLIFY_TOLERANCE'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_GEOLEVELS.CENTROIDS_TABLE' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_GEOLEVELS', @level2type=N'COLUMN',@level2name=N'CENTROIDS_TABLE'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_GEOLEVELS.CENTROIDS_AREA_ID_COLUMN' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_GEOLEVELS', @level2type=N'COLUMN',@level2name=N'CENTROIDS_AREA_ID_COLUMN'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_GEOLEVELS.AVG_NPOINTS_GEOM' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_GEOLEVELS', @level2type=N'COLUMN',@level2name=N'AVG_NPOINTS_GEOM'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_GEOLEVELS.AVG_NPOINTS_OPT' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_GEOLEVELS', @level2type=N'COLUMN',@level2name=N'AVG_NPOINTS_OPT'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_GEOLEVELS.FILE_GEOJSON_LEN' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_GEOLEVELS', @level2type=N'COLUMN',@level2name=N'FILE_GEOJSON_LEN'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_GEOLEVELS.LEG_GEOM' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_GEOLEVELS', @level2type=N'COLUMN',@level2name=N'LEG_GEOM'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_GEOLEVELS.LEG_OPT' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_GEOLEVELS', @level2type=N'COLUMN',@level2name=N'LEG_OPT'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_GEOLEVELS.COVARIATE_TABLE' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_GEOLEVELS', @level2type=N'COLUMN',@level2name=N'COVARIATE_TABLE'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_GEOLEVELS.RESTRICTED' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_GEOLEVELS', @level2type=N'COLUMN',@level2name=N'RESTRICTED'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_GEOLEVELS.RESOLUTION' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_GEOLEVELS', @level2type=N'COLUMN',@level2name=N'RESOLUTION'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_GEOLEVELS.COMPAREA' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_GEOLEVELS', @level2type=N'COLUMN',@level2name=N'COMPAREA'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_GEOLEVELS.LISTING' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_GEOLEVELS', @level2type=N'COLUMN',@level2name=N'LISTING'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_GEOLEVELS' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_GEOLEVELS'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_GEOLEVELS.T_RIF40_GEOLEVELS_PK' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_GEOLEVELS', @level2type=N'CONSTRAINT',@level2name=N'T_RIF40_GEOLEVELS_PK'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_GEOLEVELS.T_RIF40_GEOL_COMPAREA_CK' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_GEOLEVELS', @level2type=N'CONSTRAINT',@level2name=N'T_RIF40_GEOL_COMPAREA_CK'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_GEOLEVELS.T_RIF40_GEOL_LISTING_CK' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_GEOLEVELS', @level2type=N'CONSTRAINT',@level2name=N'T_RIF40_GEOL_LISTING_CK'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_GEOLEVELS.T_RIF40_GEOL_RESOLUTION_CK' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_GEOLEVELS', @level2type=N'CONSTRAINT',@level2name=N'T_RIF40_GEOL_RESOLUTION_CK'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_SSMA_SOURCE', @value=N'RIF40.T_RIF40_GEOLEVELS.T_RIF40_GEOL_RESTRICTED_CK' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'T_RIF40_GEOLEVELS', @level2type=N'CONSTRAINT',@level2name=N'T_RIF40_GEOL_RESTRICTED_CK'
GO

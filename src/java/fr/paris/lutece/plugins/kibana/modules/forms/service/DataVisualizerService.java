/*
 * Copyright (c) 2002-2021, City of Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.kibana.modules.forms.service;

import java.util.List;
import fr.paris.lutece.plugins.elasticdata.business.DataSource;
import fr.paris.lutece.plugins.elasticdata.modules.forms.business.OptionalQuestionIndexation;
import fr.paris.lutece.plugins.elasticdata.modules.forms.business.OptionalQuestionIndexationHome;
import fr.paris.lutece.plugins.forms.business.Form;
import fr.paris.lutece.plugins.forms.business.FormHome;
import fr.paris.lutece.plugins.forms.business.Question;
import fr.paris.lutece.plugins.forms.business.QuestionHome;
import fr.paris.lutece.plugins.kibana.business.DashboardReference;
import fr.paris.lutece.plugins.kibana.business.GirdLayout;
import fr.paris.lutece.plugins.kibana.service.IDataVisualizerService;
import fr.paris.lutece.plugins.kibana.service.SavedObjectService;
import fr.paris.lutece.plugins.kibana.service.VisualizationService;
import fr.paris.lutece.portal.business.event.ResourceEvent;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 *
 * ResourceHistoryService
 *
 */
public class DataVisualizerService implements IDataVisualizerService
{
    public static final String DATA_SOURCE_ID = "FormsDataSource";

    @Override
    public void createOrUpdate( ResourceEvent event, DataSource dataSource )
    {
        List<Form> listForms = FormHome.getFormList( );
        String strIdIndexPattern = dataSource.getTargetIndexName( );
        for ( Form form : listForms )
        {
            JSONArray jsonArrayReference = new JSONArray( );
            JSONArray jsonArraypanelsJSON = new JSONArray( );
            String strIdDashboard = event.getIdResource( ) + "_" + form.getId( );
            GirdLayout girdLayout = new GirdLayout( );
            String strQueryFilterFormResponse = "documentTypeName.keyword : formResponse and formId : " + form.getId( );
            initDashboard( jsonArrayReference, jsonArraypanelsJSON, strIdDashboard, strIdIndexPattern, girdLayout, form.getId( ) );
            SavedObjectService.deleteDashboard( strIdDashboard );
            List<OptionalQuestionIndexation> optionalQuestionIndexations = OptionalQuestionIndexationHome
                    .getOptionalQuestionIndexationListByFormId( form.getId( ) );
            if ( optionalQuestionIndexations != null )
            {
                for ( OptionalQuestionIndexation optionalQuestionIndexation : optionalQuestionIndexations )
                {
                    int idQuestion = optionalQuestionIndexation.getIdQuestion( );
                    Question question = QuestionHome.findByPrimaryKey( idQuestion );
                    String strType = question.getEntry( ).getEntryType( ).getBeanName( );
                    String fieldName = getFieldName( strType, question.getId( ), question.getTitle( ) );
                    if ( strType.contains( "entryTypeSelect" ) || strType.contains( "entryTypeCheckBox" ) || strType.contains( "entryTypeRadioButton" ) )
                    {
                        DashboardReference dashboardReferenceLens = new DashboardReference( "lens" );
                        jsonArrayReference.add( dashboardReferenceLens );
                        jsonArraypanelsJSON.add( SavedObjectService.createPanelJson( dashboardReferenceLens.getName( ), girdLayout.getPos( 12, 10 ) ) );
                        VisualizationService.createDonutLensVisualization( question.getTitle( ), fieldName, dashboardReferenceLens.getId( ), strIdIndexPattern,
                                strQueryFilterFormResponse );
                        DashboardReference dashboardReferenceTable = new DashboardReference( );
                        jsonArrayReference.add( dashboardReferenceTable );
                        jsonArraypanelsJSON.add( SavedObjectService.createPanelJson( dashboardReferenceTable.getName( ), girdLayout.getPos( 12, 10 ) ) );
                        VisualizationService.createDataTableTopValueVisualization( question.getTitle( ), fieldName, dashboardReferenceTable.getId( ),
                                strIdIndexPattern, strQueryFilterFormResponse );
                    }
                    if ( strType.contains( "entryTypeGeolocation" ) )
                    {
                        DashboardReference dashboardReferenceMap = new DashboardReference( "map" );
                        jsonArrayReference.add( dashboardReferenceMap );
                        jsonArraypanelsJSON.add( SavedObjectService.createPanelJson( dashboardReferenceMap.getName( ), girdLayout.getPos( 24, 10 ) ) );
                        VisualizationService.createMapVisualization( question.getTitle( ), fieldName, dashboardReferenceMap.getId( ), strIdIndexPattern,
                                strQueryFilterFormResponse );
                        DashboardReference dashboardReferenceTable = new DashboardReference( );
                        jsonArrayReference.add( dashboardReferenceTable );
                        jsonArraypanelsJSON.add( SavedObjectService.createPanelJson( dashboardReferenceTable.getName( ), girdLayout.getPos( 24, 10 ) ) );
                        VisualizationService.createDataTableTopValueVisualization( question.getTitle( ),
                                fieldName.replace( "elastic.geopoint", "address.keyword" ), dashboardReferenceTable.getId( ), strIdIndexPattern,
                                strQueryFilterFormResponse );
                    }
                    if ( strType.contains( "entryTypeText" ) || strType.contains( "entryTypeTextArea" ) )
                    {
                        DashboardReference dashboardReferenceTable = new DashboardReference( );
                        jsonArrayReference.add( dashboardReferenceTable );
                        jsonArraypanelsJSON.add( SavedObjectService.createPanelJson( dashboardReferenceTable.getName( ), girdLayout.getPos( 24, 10 ) ) );
                        VisualizationService.createDataTableTopValueVisualization( question.getTitle( ), fieldName, dashboardReferenceTable.getId( ),
                                strIdIndexPattern, strQueryFilterFormResponse );
                    }
                }
            }
            SavedObjectService.createDashboard( strIdDashboard, form.getTitle( ), strIdIndexPattern, jsonArrayReference, jsonArraypanelsJSON, dataSource );
        }
    }

    @Override
    public Boolean isExistDataSourceDataVisualizer( DataSource datasource )
    {
        if ( datasource.getId( ).equals( DATA_SOURCE_ID ) )
        {
            return true;
        }
        return false;
    }

    private static void initDashboard( JSONArray jsonArrayReference, JSONArray jsonArraypanelsJSON, String strIdDashboard, String strIdIndexPattern,
            GirdLayout girdLayout, int formId )
    {
        DashboardReference dashboardReference = new DashboardReference( );
        jsonArrayReference.add( dashboardReference );
        jsonArraypanelsJSON.add( SavedObjectService.createPanelJson( dashboardReference.getName( ), girdLayout.getPos( 36, 10 ) ) );
        createHistoResponseVisualization( "Histogramme des demandes", dashboardReference.getId( ), strIdIndexPattern, formId );
        dashboardReference = new DashboardReference( );
        jsonArrayReference.add( dashboardReference );
        jsonArraypanelsJSON.add( SavedObjectService.createPanelJson( dashboardReference.getName( ), girdLayout.getPos( 12, 10 ) ) );
        createResponseByMonthVisualization( "Répartition par mois", dashboardReference.getId( ), strIdIndexPattern, formId );
        dashboardReference = new DashboardReference( );
        jsonArrayReference.add( dashboardReference );
        jsonArraypanelsJSON.add( SavedObjectService.createPanelJson( dashboardReference.getName( ), girdLayout.getPos( 24, 10 ) ) );
        createWorkflowDurationVisualization( "Répartition par état des demandes", dashboardReference.getId( ), strIdIndexPattern, formId );
        dashboardReference = new DashboardReference( );
        jsonArrayReference.add( dashboardReference );
        jsonArraypanelsJSON.add( SavedObjectService.createPanelJson( dashboardReference.getName( ), girdLayout.getPos( 24, 10 ) ) );
        createWorkflowHistoryDurationVisualization( "Répartition de l'historique des workflows par état", dashboardReference.getId( ), strIdIndexPattern,
                formId );
    }

    private static String getFieldName( String strEntryType, int strIdQuestion, String strQuestionTitle )
    {
        String strFieldName = "userResponses." + strIdQuestion + "." + strQuestionTitle;
        if ( strEntryType.contains( "entryTypeSelect" ) || strEntryType.contains( "entryTypeRadioButton" ) )
        {
            strFieldName += ".answer_choice.keyword";
        }
        if ( strEntryType.contains( "entryTypeCheckBox" ) )
        {
            strFieldName = "userResponsesMultiValued." + strIdQuestion + "." + strQuestionTitle + ".keyword";
        }
        if ( strEntryType.contains( "entryTypeText" ) || strEntryType.contains( "entryTypeTextArea" ) )
        {
            strFieldName += ".keyword";
        }
        if ( strEntryType.contains( "entryTypeGeolocation" ) )
        {
            strFieldName += ".elastic.geopoint";
        }
        return strFieldName;
    }

    /**
     * create form response histogram visualization
     * 
     * @param visualization
     *            visualisation
     */
    private static void createHistoResponseVisualization( String strTitle, String strIdVisualization, String strIdIndexPattern, int nFormId )
    {
        JSONObject bar = VisualizationService.getVisualizationObject( strTitle, strIdVisualization, strIdIndexPattern,
                "documentTypeName.keyword : formResponse and formId : " + nFormId );
        String visState = "{\\\"title\\\":\\\"Histogramme des demandes\\\",\\\"type\\\":\\\"histogram\\\",\\\"aggs\\\":[{\\\"id\\\":\\\"1\\\",\\\"enabled\\\":true,\\\"type\\\":\\\"count\\\",\\\"params\\\":{\\\"customLabel\\\":\\\"Nombre de demandes\\\"},\\\"schema\\\":\\\"metric\\\"},{\\\"id\\\":\\\"2\\\",\\\"enabled\\\":true,\\\"type\\\":\\\"date_histogram\\\",\\\"params\\\":{\\\"field\\\":\\\"timestamp\\\",\\\"timeRange\\\":{\\\"from\\\":\\\"now-1y\\\",\\\"to\\\":\\\"now\\\"},\\\"useNormalizedEsInterval\\\":true,\\\"scaleMetricValues\\\":false,\\\"interval\\\":\\\"auto\\\",\\\"drop_partials\\\":false,\\\"min_doc_count\\\":1,\\\"extended_bounds\\\":{},\\\"customLabel\\\":\\\" \\\"},\\\"schema\\\":\\\"segment\\\"},{\\\"id\\\":\\\"3\\\",\\\"enabled\\\":true,\\\"type\\\":\\\"terms\\\",\\\"params\\\":{\\\"field\\\":\\\"workflowState.keyword\\\",\\\"orderBy\\\":\\\"1\\\",\\\"order\\\":\\\"desc\\\",\\\"size\\\":150,\\\"otherBucket\\\":false,\\\"otherBucketLabel\\\":\\\"Other\\\",\\\"missingBucket\\\":true,\\\"missingBucketLabel\\\":\\\"Sans workflow\\\",\\\"customLabel\\\":\\\"Statut\\\"},\\\"schema\\\":\\\"group\\\"}],\\\"params\\\":{\\\"type\\\":\\\"histogram\\\",\\\"grid\\\":{\\\"categoryLines\\\":false},\\\"categoryAxes\\\":[{\\\"id\\\":\\\"CategoryAxis-1\\\",\\\"type\\\":\\\"category\\\",\\\"position\\\":\\\"bottom\\\",\\\"show\\\":true,\\\"style\\\":{},\\\"scale\\\":{\\\"type\\\":\\\"linear\\\"},\\\"labels\\\":{\\\"show\\\":true,\\\"filter\\\":true,\\\"truncate\\\":100},\\\"title\\\":{}}],\\\"valueAxes\\\":[{\\\"id\\\":\\\"ValueAxis-1\\\",\\\"name\\\":\\\"LeftAxis-1\\\",\\\"type\\\":\\\"value\\\",\\\"position\\\":\\\"left\\\",\\\"show\\\":true,\\\"style\\\":{},\\\"scale\\\":{\\\"type\\\":\\\"linear\\\",\\\"mode\\\":\\\"normal\\\"},\\\"labels\\\":{\\\"show\\\":true,\\\"rotate\\\":0,\\\"filter\\\":false,\\\"truncate\\\":100},\\\"title\\\":{\\\"text\\\":\\\"Nombre de demandes\\\"}}],\\\"seriesParams\\\":[{\\\"show\\\":true,\\\"type\\\":\\\"histogram\\\",\\\"mode\\\":\\\"stacked\\\",\\\"data\\\":{\\\"label\\\":\\\"Nombre de demandes\\\",\\\"id\\\":\\\"1\\\"},\\\"valueAxis\\\":\\\"ValueAxis-1\\\",\\\"drawLinesBetweenPoints\\\":true,\\\"lineWidth\\\":2,\\\"showCircles\\\":true}],\\\"addTooltip\\\":true,\\\"addLegend\\\":true,\\\"legendPosition\\\":\\\"right\\\",\\\"times\\\":[],\\\"addTimeMarker\\\":true,\\\"labels\\\":{\\\"show\\\":true},\\\"thresholdLine\\\":{\\\"show\\\":false,\\\"value\\\":10,\\\"width\\\":1,\\\"style\\\":\\\"full\\\",\\\"color\\\":\\\"#E7664C\\\"}}}";
        ( (JSONObject) bar.get( "attributes" ) ).put( "visState", visState );
        SavedObjectService.create( bar );
    }

    /**
     * create form response histogram visualization
     * 
     * @param visualization
     *            visualisation
     */
    private static void createWorkflowDurationVisualization( String strTitle, String strIdVisualization, String strIdIndexPattern, int nFormId )
    {
        JSONObject bar = VisualizationService.getVisualizationObject( strTitle, strIdVisualization, strIdIndexPattern,
                "documentTypeName.keyword : formResponse and formId : " + nFormId );
        String visState = "{\\\"title\\\":\\\"" + strTitle
                + "\\\",\\\"type\\\":\\\"table\\\",\\\"aggs\\\":[{\\\"id\\\":\\\"1\\\",\\\"enabled\\\":true,\\\"type\\\":\\\"count\\\",\\\"params\\\":{\\\"customLabel\\\":\\\"Nombre de demandes\\\"},\\\"schema\\\":\\\"metric\\\"},{\\\"id\\\":\\\"2\\\",\\\"enabled\\\":true,\\\"type\\\":\\\"terms\\\",\\\"params\\\":{\\\"field\\\":\\\"workflowState.keyword\\\",\\\"orderBy\\\":\\\"1\\\",\\\"order\\\":\\\"desc\\\",\\\"size\\\":150,\\\"otherBucket\\\":false,\\\"otherBucketLabel\\\":\\\"Other\\\",\\\"missingBucket\\\":false,\\\"missingBucketLabel\\\":\\\"Missing\\\",\\\"customLabel\\\":\\\"Etat du workflow\\\"},\\\"schema\\\":\\\"bucket\\\"},{\\\"id\\\":\\\"3\\\",\\\"enabled\\\":true,\\\"type\\\":\\\"avg\\\",\\\"params\\\":{\\\"field\\\":\\\"completeDuration\\\",\\\"customLabel\\\":\\\"Durée moyenne avant l'état\\\"},\\\"schema\\\":\\\"metric\\\"}],\\\"params\\\":{\\\"perPage\\\":150,\\\"showPartialRows\\\":false,\\\"showMetricsAtAllLevels\\\":false,\\\"sort\\\":{\\\"columnIndex\\\":null,\\\"direction\\\":null},\\\"showTotal\\\":false,\\\"totalFunc\\\":\\\"sum\\\",\\\"percentageCol\\\":\\\"Nombre de demandes\\\"}}";
        ( (JSONObject) bar.get( "attributes" ) ).put( "visState", visState );
        SavedObjectService.create( bar );
    }

    /**
     * create form response history histogram visualization
     * 
     * @param visualization
     *            visualisation
     */
    private static void createWorkflowHistoryDurationVisualization( String strTitle, String strIdVisualization, String strIdIndexPattern, int nFormId )
    {
        JSONObject bar = VisualizationService.getVisualizationObject( strTitle, strIdVisualization, strIdIndexPattern,
                "documentTypeName.keyword : formResponseHistory and formId : " + nFormId );
        String visState = "{\\\"title\\\":\\\"" + strTitle
                + "\\\",\\\"type\\\":\\\"table\\\",\\\"aggs\\\":[{\\\"id\\\":\\\"1\\\",\\\"enabled\\\":true,\\\"type\\\":\\\"count\\\",\\\"params\\\":{\\\"customLabel\\\":\\\"Nombre d'historique\\\"},\\\"schema\\\":\\\"metric\\\"},{\\\"id\\\":\\\"2\\\",\\\"enabled\\\":true,\\\"type\\\":\\\"terms\\\",\\\"params\\\":{\\\"field\\\":\\\"workflowState.keyword\\\",\\\"orderBy\\\":\\\"1\\\",\\\"order\\\":\\\"desc\\\",\\\"size\\\":150,\\\"otherBucket\\\":false,\\\"otherBucketLabel\\\":\\\"Other\\\",\\\"missingBucket\\\":false,\\\"missingBucketLabel\\\":\\\"Missing\\\",\\\"customLabel\\\":\\\"Etat du workflow\\\"},\\\"schema\\\":\\\"bucket\\\"},{\\\"id\\\":\\\"3\\\",\\\"enabled\\\":true,\\\"type\\\":\\\"avg\\\",\\\"params\\\":{\\\"field\\\":\\\"completeDuration\\\",\\\"customLabel\\\":\\\"Durée moyenne avant l'état\\\"},\\\"schema\\\":\\\"metric\\\"},{\\\"id\\\":\\\"4\\\",\\\"enabled\\\":true,\\\"type\\\":\\\"avg\\\",\\\"params\\\":{\\\"field\\\":\\\"taskDuration\\\",\\\"customLabel\\\":\\\"Durée moyenne état à état\\\"},\\\"schema\\\":\\\"metric\\\"}],\\\"params\\\":{\\\"perPage\\\":150,\\\"showPartialRows\\\":false,\\\"showMetricsAtAllLevels\\\":false,\\\"sort\\\":{\\\"columnIndex\\\":null,\\\"direction\\\":null},\\\"showTotal\\\":false,\\\"totalFunc\\\":\\\"sum\\\",\\\"percentageCol\\\":\\\"\\\"}}";
        ( (JSONObject) bar.get( "attributes" ) ).put( "visState", visState );
        SavedObjectService.create( bar );
    }

    /**
     * create form response by month datatable visualization
     * 
     * @param visualization
     *            visualisation
     */
    private static void createResponseByMonthVisualization( String strTitle, String strIdVisualization, String strIdIndexPattern, int nFormId )
    {
        JSONObject bar = VisualizationService.getVisualizationObject( strTitle, strIdVisualization, strIdIndexPattern,
                "documentTypeName.keyword : formResponse and formId : " + nFormId );
        String visState = "{\\\"title\\\":\\\"" + strTitle
                + "\\\",\\\"type\\\":\\\"table\\\",\\\"aggs\\\":[{\\\"id\\\":\\\"1\\\",\\\"enabled\\\":true,\\\"type\\\":\\\"count\\\",\\\"params\\\":{\\\"customLabel\\\":\\\"Nombre de réponses\\\"},\\\"schema\\\":\\\"metric\\\"},{\\\"id\\\":\\\"2\\\",\\\"enabled\\\":true,\\\"type\\\":\\\"date_histogram\\\",\\\"params\\\":{\\\"field\\\":\\\"timestamp\\\",\\\"timeRange\\\":{\\\"from\\\":\\\"now-90d\\\",\\\"to\\\":\\\"now\\\"},\\\"useNormalizedEsInterval\\\":true,\\\"scaleMetricValues\\\":false,\\\"interval\\\":\\\"M\\\",\\\"drop_partials\\\":false,\\\"min_doc_count\\\":1,\\\"extended_bounds\\\":{},\\\"customLabel\\\":\\\"Mois\\\"},\\\"schema\\\":\\\"bucket\\\"}],\\\"params\\\":{\\\"perPage\\\":10,\\\"showPartialRows\\\":false,\\\"showMetricsAtAllLevels\\\":false,\\\"sort\\\":{\\\"columnIndex\\\":null,\\\"direction\\\":null},\\\"showTotal\\\":false,\\\"totalFunc\\\":\\\"sum\\\",\\\"percentageCol\\\":\\\"\\\",\\\"row\\\":false}}";
        ( (JSONObject) bar.get( "attributes" ) ).put( "visState", visState );
        SavedObjectService.create( bar );
    }
}

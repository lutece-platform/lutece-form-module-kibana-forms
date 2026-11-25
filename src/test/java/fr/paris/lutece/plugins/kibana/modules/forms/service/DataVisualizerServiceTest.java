/*
 * Copyright (c) 2002-2026, City of Paris
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.elasticdata.modules.forms.business.FormsDataSource;
import fr.paris.lutece.plugins.elasticdata.modules.forms.business.OptionalQuestionIndexation;
import fr.paris.lutece.plugins.elasticdata.modules.forms.business.OptionalQuestionIndexationHome;
import fr.paris.lutece.plugins.forms.business.Form;
import fr.paris.lutece.plugins.forms.business.FormHome;
import fr.paris.lutece.plugins.kibana.business.Dashboard;
import fr.paris.lutece.plugins.kibana.business.DashboardHome;
import fr.paris.lutece.test.LuteceTestCase;
import jakarta.inject.Inject;

/**
 * Dashboard generation after a full indexing (Kibana is not reachable in the tests: the calls fail and are logged, the dashboards are still
 * recorded)
 */
public class DataVisualizerServiceTest extends LuteceTestCase
{
    @Inject
    private DataVisualizerService _service;
    @Inject
    private FormsDataSource _dataSource;

    private Form _formWithDeletedQuestion;
    private Form _formAfter;
    private OptionalQuestionIndexation _orphan;

    @BeforeEach
    protected void createForms( )
    {
        _formWithDeletedQuestion = createForm( "DataVisualizerServiceTest deleted question" );
        _formAfter = createForm( "DataVisualizerServiceTest after" );
        // a question ticked for the indexation, then deleted in the forms editor: the indexation row stays
        _orphan = new OptionalQuestionIndexation( );
        _orphan.setIdForm( _formWithDeletedQuestion.getId( ) );
        _orphan.setIdQuestion( 999999 );
        OptionalQuestionIndexationHome.create( _orphan );
    }

    @AfterEach
    protected void removeForms( )
    {
        OptionalQuestionIndexationHome.remove( _orphan.getId( ) );
        for ( Form form : new Form [ ] {
                _formWithDeletedQuestion, _formAfter
        } )
        {
            Dashboard dashboard = DashboardHome.findByKibanaId( _dataSource.getId( ) + "_" + form.getId( ) );
            if ( dashboard != null )
            {
                DashboardHome.delete( dashboard.getId( ) );
            }
            FormHome.remove( form.getId( ) );
        }
    }

    /**
     * A deleted question left in the indexation settings neither stops the generation nor deprives the other forms of their dashboard
     */
    @Test
    public void testDeletedIndexedQuestionDoesNotStopTheGeneration( )
    {
        _service.createOrUpdate( _dataSource );

        assertNotNull( "dashboard of the form with the deleted question",
                DashboardHome.findByKibanaId( _dataSource.getId( ) + "_" + _formWithDeletedQuestion.getId( ) ) );
        assertNotNull( "dashboard of the next form", DashboardHome.findByKibanaId( _dataSource.getId( ) + "_" + _formAfter.getId( ) ) );
    }

    private static Form createForm( String strTitle )
    {
        Form form = new Form( );
        form.setTitle( strTitle );
        form.setBreadcrumbName( "forms.horizontalBreadcrumb" );
        FormHome.create( form );

        return form;
    }
}

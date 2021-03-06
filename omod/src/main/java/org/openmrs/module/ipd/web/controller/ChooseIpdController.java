/**
 *  Copyright 2010 Society for Health Information Systems Programmes, India (HISP India)
 *
 *  This file is part of IPD module.
 *
 *  IPD module is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.

 *  IPD module is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with IPD module.  If not, see <http://www.gnu.org/licenses/>.
 *
 **/

package org.openmrs.module.ipd.web.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptName;
import org.openmrs.GlobalProperty;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.ipd.util.IpdConstants;
import org.openmrs.module.ipd.util.IpdUtils;
import org.openmrs.module.hospitalcore.HospitalCoreService;
import org.openmrs.module.hospitalcore.util.ConceptAnswerComparator;
import org.openmrs.module.hospitalcore.util.HospitalCoreConstants;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller("ChooseIpdController")
@RequestMapping("/module/ipd/chooseIpdWard.htm")
public class ChooseIpdController {
	
	@RequestMapping(method=RequestMethod.GET)
	public String firstView(
			@RequestParam(value ="ipdWard",required=false) String[] ipdWard, 
			@RequestParam(value ="tab",required=false) Integer tab, 
			Model model){
		creatConceptQuestionAndAnswer(Context.getConceptService() , Context.getAuthenticatedUser() ,HospitalCoreConstants.CONCEPT_ADMISSION_OUTCOME,  new String[]{ "Improve", "Cured" , "Discharge on request" ,"LAMA", "Absconding", "Death"});
		Concept ipdConcept = Context.getConceptService().getConceptByName(Context.getAdministrationService().getGlobalProperty(IpdConstants.PROPERTY_IPDWARD));
		List<ConceptAnswer> list = (ipdConcept!= null ?  new ArrayList<ConceptAnswer>(ipdConcept.getAnswers()) : null);
		if(CollectionUtils.isNotEmpty(list)){
			Collections.sort(list, new ConceptAnswerComparator());
		}
		model.addAttribute("listIpd", list);
		String doctorRoleProps = Context.getAdministrationService().getGlobalProperty(IpdConstants.PROPERTY_NAME_DOCTOR_ROLE);
		Role doctorRole = Context.getUserService().getRole(doctorRoleProps);
		if( doctorRole != null ){
			List<User> listDoctor = Context.getUserService().getUsersByRole(doctorRole);
			model.addAttribute("listDoctor",listDoctor);
		}
		tab =0;
		model.addAttribute("ipdWard",ipdWard);
		model.addAttribute("tab",tab.intValue());
		return "module/ipd/chooseIpdWard";
	}
	private Concept insertConcept(ConceptService conceptService,
			String dataTypeName, String conceptClassName, String concept) {
		try {
			ConceptDatatype datatype = Context.getConceptService()
					.getConceptDatatypeByName(dataTypeName);
			ConceptClass conceptClass = conceptService
					.getConceptClassByName(conceptClassName);
			Concept con = conceptService.getConcept(concept);
			// System.out.println(con);
			if (con == null) {
				con = new Concept();
				ConceptName name = new ConceptName(concept,
						Context.getLocale());
				con.addName(name);
				con.setDatatype(datatype);
				con.setConceptClass(conceptClass);
				return conceptService.saveConcept(con);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	public void creatConceptQuestionAndAnswer(ConceptService conceptService,  User user ,String conceptParent, String...conceptChild) {
		// System.out.println("========= insertExternalHospitalConcepts =========");
		Concept concept = conceptService.getConcept(conceptParent);
		if(concept == null){
			insertConcept(conceptService, "Coded", "Question" , conceptParent);
		}
		if (concept != null) {
			
			for (String hn : conceptChild) {
				insertHospital(conceptService, hn);
			}
			addConceptAnswers(concept, conceptChild, user);
		}
	}
	
	private void addConceptAnswers(Concept concept, String[] answerNames,
			User creator) {
		Set<Integer> currentAnswerIds = new HashSet<Integer>();
		for (ConceptAnswer answer : concept.getAnswers()) {
			currentAnswerIds.add(answer.getAnswerConcept().getConceptId());
		}
		boolean changed = false;
		for (String answerName : answerNames) {
			Concept answer = Context.getConceptService().getConcept(answerName);
			if (!currentAnswerIds.contains(answer.getConceptId())) {
				changed = true;
				ConceptAnswer conceptAnswer = new ConceptAnswer(answer);
				conceptAnswer.setCreator(creator);
				concept.addAnswer(conceptAnswer);
			}
		}
		if (changed) {
			Context.getConceptService().saveConcept(concept);
		}
	}
	private Concept insertHospital(ConceptService conceptService,
			String hospitalName) {
		try {
			ConceptDatatype datatype = Context.getConceptService()
					.getConceptDatatypeByName("N/A");
			ConceptClass conceptClass = conceptService
					.getConceptClassByName("Misc");
			Concept con = conceptService.getConceptByName(hospitalName);
			// System.out.println(con);
			if (con == null) {
				con = new Concept();
				ConceptName name = new ConceptName(hospitalName,
						Context.getLocale());
				con.addName(name);
				con.setDatatype(datatype);
				con.setConceptClass(conceptClass);
				return conceptService.saveConcept(con);
			}
			return con;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	

}
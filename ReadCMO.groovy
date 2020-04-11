@Grapes([
    @Grab(group='org.semanticweb.elk', module='elk-owlapi', version='0.4.3'),
    @Grab(group='net.sourceforge.owlapi', module='owlapi-api', version='5.1.13'),
    @Grab(group='net.sourceforge.owlapi', module='owlapi-apibinding', version='5.1.13'),
    @Grab(group='net.sourceforge.owlapi', module='owlapi-impl', version='5.1.13'),
    @Grab(group='net.sourceforge.owlapi', module='owlapi-parsers', version='5.1.13'),
    @Grab(group='net.sourceforge.owlapi', module='owlapi-distribution', version='5.1.13'),
    @GrabConfig(systemClassLoader=true)
])

import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.parameters.*
import org.semanticweb.elk.owlapi.ElkReasonerFactory
import org.semanticweb.elk.owlapi.ElkReasonerConfiguration
import org.semanticweb.elk.reasoner.config.*
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.reasoner.*
import org.semanticweb.owlapi.reasoner.structural.StructuralReasoner
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary
import org.semanticweb.owlapi.model.*
import org.semanticweb.owlapi.io.*
import org.semanticweb.owlapi.owllink.*
import org.semanticweb.owlapi.util.*
import org.semanticweb.owlapi.search.*
import org.semanticweb.owlapi.manchestersyntax.renderer.*
import org.semanticweb.owlapi.reasoner.structural.*
import java.util.regex.Pattern

def and = { cl1, cl2 ->
    fac.getOWLObjectIntersectionOf(cl1,cl2)
}
def some = { r, cl ->
    fac.getOWLObjectSomeValuesFrom(r,cl)
}
def equiv = { cl1, cl2 ->
    fac.getOWLEquivalentClassesAxiom(cl1, cl2)
}
def subclass = { cl1, cl2 ->
    fac.getOWLSubClassOfAxiom(cl1, cl2)
}
def r = { String s ->
    if (s == "part-of") {
	fac.getOWLObjectProperty(IRI.create("http://purl.obolibrary.org/obo/BFO_0000050"))
    } else if (s == "has-part") {
	fac.getOWLObjectProperty(IRI.create("http://purl.obolibrary.org/obo/BFO_0000051"))
    } else if (s == "inheres-in-part-of") {
	fac.getOWLObjectProperty(IRI.create("http://purl.obolibrary.org/obo/RO_0002314"))
    } else if (s == "inheres-in") {
	fac.getOWLObjectProperty(IRI.create("http://purl.obolibrary.org/obo/RO_0000052"))
    } else {
	fac.getOWLObjectProperty(IRI.create("http://aber-owl.net/#"+s))
    }
}
def c = { String s ->
    if (s == "quality") {
	fac.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/PATO_0000001"))
    } else {
	fac.getOWLClass(IRI.create(onturi+s))
    }
}
//def add = { OWLOntology o, OWLAxiom a -> this.manager.addAxiom(o, a) }


def labelMap = { ont ->
    def map = [:]
    def fac = OWLManager.createOWLOntologyManager().getOWLDataFactory()
    ont.getClassesInSignature(true).each { cl ->
	EntitySearcher.getAnnotations(cl, ont, fac.getRDFSLabel()).each { anno ->
	    OWLAnnotationValue val = anno.getValue()
	    if (val instanceof OWLLiteral) {
		map[cl] = val.getLiteral()
	    }
	}
    }
    map
}

def manager = OWLManager.createOWLOntologyManager()
fac = manager.getOWLDataFactory()

def ont = manager.createOntology(IRI.create("http://biohackathon.org/covid19/cmo-ext.owl"))

def cmo = manager.loadOntologyFromOntologyDocument(new File("cmo.owl"))
def uberon = manager.loadOntologyFromOntologyDocument(new File("uberon.owl"))
def chebi = manager.loadOntologyFromOntologyDocument(new File("chebi.owl"))
def pato = manager.loadOntologyFromOntologyDocument(new File("pato.owl"))
// ConsoleProgressMonitor progressMonitor = new ConsoleProgressMonitor()
// OWLReasonerConfiguration config = new SimpleConfiguration(progressMonitor)
// ElkReasonerFactory f1 = new ElkReasonerFactory()
// def cmoreasoner = f1.createReasoner(cmo,config)
// def uberonreasoner = f1.createReasoner(uberon,config)
// def chebireasoner = f1.createReasoner(chebi,config)
// def patoreasoner = f1.createReasoner(pato,config)


def cmolabels = labelMap(cmo)
def uberonlabels = labelMap(uberon)
def chebilabels = labelMap(chebi)
def patolabels = labelMap(pato)

def cmo2other = [:].withDefault { new LinkedHashSet() }
def cmo2uberon = [:].withDefault { new LinkedHashSet() }
def cmo2pato = [:].withDefault { new LinkedHashSet() }
def cmo2chebi = [:].withDefault { new LinkedHashSet() }


cmolabels.each { cl, lab ->
    uberonlabels.each { k, v ->
	v = Pattern.quote(v)
	if (lab=~/(^|\s)${v}($|\s)/) {
	    println "$lab\t$k\t$cl"
	    cmo2other[cl].add(k)
	    cmo2uberon[cl].add(k)
	}
    }
    patolabels.each { k, v ->
	v = Pattern.quote(v)
	if (lab=~/(^|\s)${v}($|\s)/) {
    	    println "$lab\t$k\t$cl"
    	    cmo2other[cl].add(k)
	    cmo2pato[cl].add(k)
    	}
    }
    chebilabels.each { k, v ->
	v = Pattern.quote(v)
	if (lab=~/(^|\s)${v}($|\s)/) {
    	    println "$lab\t$k\t$cl"
    	    cmo2other[cl].add(k)
	    cmo2chebi[cl].add(k)
    	}
    }
}



cmo.getClassesInSignature(true).each { k ->
    if (cmo2uberon[k] != null) {
	v = cmo2uberon[k]
	v.each { cl ->
	    manager.addAxiom(ont, subclass(k, some(r("has-part"), and(c("quality"), some(r("inheres-in-part-of"), cl)))))
	}
    }
    if (cmo2uberon[k] != null && cmo2pato[k] != null) {
	v1 = cmo2uberon[k]
	v2 = cmo2pato[k]
	v1.each { cl1 ->
	    v2.each { cl2 ->
		manager.addAxiom(ont, subclass(k, some(r("has-part"), and(cl2, some(r("inheres-in"), cl1)))))
	    }
	}
    }
}



manager.applyChange(new AddImport(ont, fac.getOWLImportsDeclaration(IRI.create("http://purl.obolibrary.org/obo/mp.owl"))))
manager.applyChange(new AddImport(ont, fac.getOWLImportsDeclaration(IRI.create("http://purl.obolibrary.org/obo/pato.owl"))))
manager.applyChange(new AddImport(ont, fac.getOWLImportsDeclaration(IRI.create("http://purl.obolibrary.org/obo/uberon.owl"))))
manager.applyChange(new AddImport(ont, fac.getOWLImportsDeclaration(IRI.create("http://purl.obolibrary.org/obo/chebi.owl"))))
manager.applyChange(new AddImport(ont, fac.getOWLImportsDeclaration(IRI.create("http://purl.obolibrary.org/obo/cmo.owl"))))

manager.saveOntology(ont, IRI.create(new File("qt.owl").toURI()))

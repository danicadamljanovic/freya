package org.freya.service;

import org.freya.index.solr.TripleIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;


@Controller
@RequestMapping("solr")
public class SolrIndexService {

    private final static Logger log = LoggerFactory.getLogger(SolrIndexService.class);

    @Autowired
    private TripleIndexer tripleIndexer;

    @RequestMapping(value = "/reindex", method = RequestMethod.POST)
    public void reindex(HttpServletResponse response, Principal principal) throws IOException {
        PrintWriter out = response.getWriter();
        response.setContentType("text/html");
        final long start = System.currentTimeMillis();
        try {
            out.write("<div>Start reindexing ... </div>");
            out.flush();
            tripleIndexer.indexAll();
        } catch(Exception e) {
            log.error("Error while reindexing", e);
            out.write(e.getLocalizedMessage());
        } finally {
            final long stop = System.currentTimeMillis();
            out.print("<div>End reindexing! Spent time: " + (stop - start) + "ms</div>");
            out.flush();
            out.close();
        }
    }
    
    @RequestMapping(value = "/clear", method = RequestMethod.POST)
    public void clear(HttpServletResponse response, Principal principal) throws IOException {
        PrintWriter out = response.getWriter();
        response.setContentType("text/html");
        final long start = System.currentTimeMillis();
        try {
            out.write("<div>Start clearing ... </div>");
            out.flush();
            tripleIndexer.clear();
        } catch(Exception e) {
            log.error("Error while clearing", e);
            out.write(e.getLocalizedMessage());
        } finally {
            final long stop = System.currentTimeMillis();
            out.print("<div>End clearing! Spent time: " + (stop - start) + "ms</div>");
            out.flush();
            out.close();
        }
    }
}

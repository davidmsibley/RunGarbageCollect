package gov.usgs.cida.rungc;

import java.io.*;
import java.util.ArrayList;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 * Pulled from http://frankkieviet.blogspot.com/2006/10/how-to-fix-dreaded-permgen-space.html
 * and then edited to work here.
 * @author dmsibley
 */
public class RunGC extends HttpServlet {

	private static class XClassloader extends ClassLoader {

		private byte[] data;
		private int len;

		public XClassloader(byte[] data, int len) {
			super(RunGC.class.getClassLoader());
			this.data = data;
			this.len = len;
		}

		public Class findClass(String name) {
			return defineClass(name, data, 0, len);
		}
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		processRequest(req, resp); //To change body of generated methods, choose Tools | Templates.
	}
	
	protected void processRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("text/html;charset=UTF-8");
		PrintWriter out = response.getWriter();
		out.println("<html><body><pre>");

		try {
// Load class data
			byte[] buf = new byte[1000000];
			InputStream inp = this.getClass().getClassLoader()
					.getResourceAsStream("gov/usgs/cida/rungc/BigFatClass.class");
			int n = inp.read(buf);
			inp.close();
			out.println(n + " bytes read of class data");

// Exhaust permgen
			ArrayList keep = new ArrayList();
			int nLoadedAtError = 0;
			try {
				for (int i = 0; i < Integer.MAX_VALUE; i++) {
					XClassloader loader = new XClassloader(buf, n);
					Class c = loader.findClass("gov.usgs.cida.rungc.BigFatClass");
					keep.add(c);
				}
			} catch (Error e) {
				nLoadedAtError = keep.size();
			}

// Release memory
			keep = null;
			out.println("Error at " + nLoadedAtError);

// Load one more; this should trigger GC
			XClassloader loader = new XClassloader(buf, n);
			Class c = loader.findClass("gov.usgs.cida.rungc.BigFatClass");
			out.println("Loaded one more");
		} catch (Exception e) {
			e.printStackTrace(out);
		}

		out.println("</pre></body></html>");
		out.close();

	}
}

/* 
 * See LICENSE in top-level directory.
 */
package com.monkygames.kbmaster.io;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates a PDF of the keybindings.
 * @version 1.0
 */
public class BindingPDFWriter{

// ============= Class variables ============== //
    /**
     * Writes a PDF file of the images passed in.
     * @param awtImage the images to write to a pdf file.
     * @param name the title of the pdf document.
     * @param subject the description of this pdf document.
     * @param keywords for this pdf.
     * @param path the path to the file to write to.
     */
    public BindingPDFWriter(java.awt.Image awtImage[], String name, 
			      String subject, String keywords, 
			      final String header, final String footer, String path){
	try {
	    Rectangle a4rot = PageSize.A4.rotate();
	    a4rot.setBackgroundColor(BaseColor.WHITE);
	    Document document = new Document(a4rot);
	    PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(path));

	    HeaderFooter headerFooter = new HeaderFooter(header,footer);
	    writer.setPageEvent(headerFooter);

	    document.open();
	    document.addTitle(name);
	    document.addSubject(subject);
	    document.addKeywords(keywords);
	    document.addCreationDate();
	    document.addAuthor("Monky Games");
	    document.addCreator("Autogenerated");
	    document.addKeywords(keywords);

	    for(int i = 0; i < awtImage.length; i++){
		Image image = Image.getInstance(writer,awtImage[i],1.0f);
		image.setAlignment(Element.ALIGN_CENTER);
		document.add(image);
	    }
	    document.close();
	} catch (BadElementException ex) {
	    Logger.getLogger(BindingPDFWriter.class.getName()).log(Level.SEVERE, null, ex);
	} catch (IOException ex) {
	    Logger.getLogger(BindingPDFWriter.class.getName()).log(Level.SEVERE, null, ex);
	} catch (DocumentException ex) {
	    Logger.getLogger(BindingPDFWriter.class.getName()).log(Level.SEVERE, null, ex);
	}
    }
// ============= Constructors ============== //
// ============= Public Methods ============== //
// ============= Protected Methods ============== //
// ============= Private Methods ============== //
// ============= Implemented Methods ============== //
// ============= Extended Methods ============== //
// ============= Internal Classes ============== //
    /**
     * Inner class to add a header and a footer.
     */
    public class HeaderFooter extends PdfPageEventHelper {
	private String header,footer;
	int pagenumber;

	public HeaderFooter(String header, String footer){
	    super();
	    this.header = header;
	    this.footer = footer;
	}

	@Override
	public void onStartPage(PdfWriter writer, Document document) {
	    pagenumber++;
	}

	@Override
	public void onEndPage(PdfWriter writer, Document document) {
	    Rectangle rect = writer.getPageSize();
	    Phrase headerP = new Phrase(header);
	    Phrase footerP = new Phrase(footer);
	    ColumnText.showTextAligned(writer.getDirectContent(),
		    Element.ALIGN_RIGHT, headerP,
		    rect.getRight()-headerP.getFont().getCalculatedSize(), rect.getTop()-16,0);
	    ColumnText.showTextAligned(writer.getDirectContent(),
		    Element.ALIGN_RIGHT, footerP,
		    rect.getRight()-footerP.getFont().getCalculatedSize(), rect.getBottom()+10, 0);
	}
    }
// ============= Static Methods ============== //

}
/*
 * Local variables:
 *  c-indent-level: 4
 *  c-basic-offset: 4
 * End:
 *
 * vim: ts=8 sts=4 sw=4 noexpandtab
 */

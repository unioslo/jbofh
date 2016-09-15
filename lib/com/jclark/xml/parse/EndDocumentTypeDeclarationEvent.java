package com.jclark.xml.parse;

/**
 * Information about the end of a document type declaration.
 * @see com.jclark.xml.parse.base.Application#endDocumentTypeDeclaration
 * @version $Revision: 1.1 $ $Date: 1998/06/10 09:45:11 $
 */
public interface EndDocumentTypeDeclarationEvent {
  /**
   * Returns the DTD that was declared.
   */
  DTD getDTD();
}

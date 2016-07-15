package com.jclark.xml.parse;

/**
 * Information about the end of an element.
 * @see com.jclark.xml.parse.base.Application#endElement
 * @version $Revision: 1.5 $ $Date: 1998/05/14 02:45:44 $
 */
public interface EndElementEvent {
  /**
   * Returns the element type name.
   */
  String getName();
  
}

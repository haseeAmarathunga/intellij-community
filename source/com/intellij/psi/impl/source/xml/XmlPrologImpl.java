package com.intellij.psi.impl.source.xml;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.impl.source.tree.ChildRole;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.xml.XmlDoctype;
import com.intellij.psi.xml.XmlProlog;
import com.intellij.lang.ASTNode;

/**
 * @author Mike
 */
public class XmlPrologImpl extends XmlElementImpl implements XmlProlog {
  private static final Logger LOG = Logger.getInstance("#com.intellij.psi.impl.source.xml.XmlPrologImpl");

  public XmlPrologImpl() {
    super(XML_PROLOG);
  }

  public void accept(PsiElementVisitor visitor) {
    visitor.visitXmlProlog(this);
  }

  public int getChildRole(ASTNode child) {
    LOG.assertTrue(child.getTreeParent() == this);
    if (child.getElementType() == XML_DOCTYPE) {
      return ChildRole.XML_DOCTYPE;
    }
    else {
      return ChildRole.NONE;
    }
  }

  public XmlDoctype getDoctype() {
    return (XmlDoctype)findChildByRoleAsPsiElement(ChildRole.XML_DOCTYPE);
  }
}

package com.intellij.psi.impl.source.tree.java;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.impl.source.tree.ChildRole;
import com.intellij.psi.impl.source.tree.CompositePsiElement;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.impl.source.tree.TreeUtil;
import com.intellij.lang.ASTNode;

public class PsiArrayAccessExpressionImpl extends CompositePsiElement implements PsiArrayAccessExpression {
  private static final Logger LOG = Logger.getInstance("#com.intellij.psi.impl.source.tree.java.PsiArrayAccessExpressionImpl");

  public PsiArrayAccessExpressionImpl() {
    super(ARRAY_ACCESS_EXPRESSION);
  }

  public PsiExpression getArrayExpression() {
    return (PsiExpression)findChildByRoleAsPsiElement(ChildRole.ARRAY);
  }

  public PsiExpression getIndexExpression() {
    return (PsiExpression)findChildByRoleAsPsiElement(ChildRole.INDEX);
  }

  public PsiType getType() {
    PsiType arrayType = getArrayExpression().getType();
    if (!(arrayType instanceof PsiArrayType)) return null;
    return ((PsiArrayType)arrayType).getComponentType();
  }

  public ASTNode findChildByRole(int role) {
    LOG.assertTrue(ChildRole.isUnique(role));
    switch(role){
      default:
        return null;

      case ChildRole.ARRAY:
        return firstChild;

      case ChildRole.INDEX:
        {
          ASTNode lbracket = findChildByRole(ChildRole.LBRACKET);
          if (lbracket == null) return null;
          for(ASTNode child = lbracket.getTreeNext(); child != null; child = child.getTreeNext()){
            if (EXPRESSION_BIT_SET.isInSet(child.getElementType())){
              return (TreeElement)child;
            }
          }
          return null;
        }

      case ChildRole.LBRACKET:
        return TreeUtil.findChild(this, LBRACKET);

      case ChildRole.RBRACKET:
        return TreeUtil.findChild(this, RBRACKET);
    }
  }

  public int getChildRole(ASTNode child) {
    LOG.assertTrue(child.getTreeParent() == this);
    IElementType i = child.getElementType();
    if (i == LBRACKET) {
      return ChildRole.LBRACKET;
    }
    else if (i == RBRACKET) {
      return ChildRole.RBRACKET;
    }
    else {
      if (EXPRESSION_BIT_SET.isInSet(child.getElementType())) {
        return child == firstChild ? ChildRole.ARRAY : ChildRole.INDEX;
      }
      else {
        return ChildRole.NONE;
      }
    }
  }

  public void accept(PsiElementVisitor visitor) {
    visitor.visitArrayAccessExpression(this);
  }

  public String toString() {
    return "PsiArrayAccessExpression:" + getText();
  }
}


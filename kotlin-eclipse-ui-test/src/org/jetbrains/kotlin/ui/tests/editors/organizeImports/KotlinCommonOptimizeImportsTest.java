package org.jetbrains.kotlin.ui.tests.editors.organizeImports;

import org.junit.Test;

public class KotlinCommonOptimizeImportsTest extends KotlinOrganizeImportsTestCase {
    @Override
    public String getTestDataRelativePath() {
        return "../common_testData/ide/editor/optimizeImports/common";
    }
    
    @Test
    public void ArrayAccessExpression() {
        doAutoTest();
    }
    
    @Test
    public void ClassMemberImported() {
        doAutoTest();
    }
    
    @Test
    public void ComponentFunction() {
        doAutoTest();
    }
    
    @Test
    public void CurrentPackage() {
        doAutoTest();
    }
    
    @Test
    public void DefaultObjectReference() {
        doAutoTest();
    }
    
    @Test
    public void Enums() {
        doAutoTest();
    }
    
    @Test
    public void InvokeFunction() {
        doAutoTest();
    }
    
    @Test
    public void IteratorFunction() {
        doAutoTest();
    }
    
    @Test
    public void KeywordNames() {
        doAutoTest();
    }
    
    @Test
    public void Kt2488EnumEntry() {
        doAutoTest();
    }
    
    @Test
    public void Kt2709() {
        doAutoTest();
    }
    
    @Test
    public void KT9875() {
        doAutoTest();
    }
    
    @Test
    public void MembersInScope() {
        doAutoTest();
    }
    
    @Test
    public void NestedClassReferenceOutsideClassBody() {
        doAutoTest();
    }

    @Test
    public void UnresolvedImport() {
        doAutoTest();
     }

    @Test
    public void Overloads() {
        doAutoTest();
     }

    @Test
    public void DefaultImportAndAlias() {
        doAutoTest();
     }

    @Test
    public void ConflictWithAlias() {
        doAutoTest();
     }

    @Test
    public void MemberImports() {
        doAutoTest();
     }

    @Test
    public void DefaultImportAndAlias2() {
        doAutoTest();
     }

    @Test
    public void TwoConstructors() {
        doAutoTest();
     }

    @Test
    public void ProvideDelegate() {
        doAutoTest();
     }

    @Test
    public void ExtensionFunctionalTypeValFromCompanionObjectNonExtCall() {
        doAutoTest();
     }

    @Test
    public void KT13689() {
        doAutoTest();
     }

    @Test
    public void WithAlias() {
        doAutoTest();
     }

    @Test
    public void ExtensionFunctionalTypeValFromCompanionObject() {
        doAutoTest();
     }

    @Test
    public void WithAlias2() {
        doAutoTest();
     }

    @Test
    public void KT11640_1() {
        doAutoTest();
     }

    @Test
    public void ConflictWithAlias2() {
        doAutoTest();
     }

    @Test
    public void ExtensionFunctionalTypeValFromCompanionObjectCallOnCompanion() {
        doAutoTest();
     }

    @Test
    public void KT11640() {
        doAutoTest();
     }

    @Test
    public void ProvideDelegate2() {
        doAutoTest();
     }
}

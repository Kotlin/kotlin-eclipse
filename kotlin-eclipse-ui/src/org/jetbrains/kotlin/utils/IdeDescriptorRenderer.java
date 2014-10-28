package org.jetbrains.kotlin.utils;

import java.util.List;

import kotlin.Function1;
import kotlin.KotlinPackage;

import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames;
import org.jetbrains.jet.lang.resolve.java.kotlinSignature.CollectionClassMapping;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.Flexibility;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.JetTypeImpl;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.TypeProjectionImpl;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.TypesPackage;
import org.jetbrains.jet.renderer.DescriptorRenderer;
import org.jetbrains.jet.renderer.DescriptorRendererBuilder;

public class IdeDescriptorRenderer {
    public static final Function1<JetType, JetType> APPROXIMATE_FLEXIBLE_TYPES = new Function1<JetType, JetType>() {
        @Override
        public JetType invoke(JetType type) {
            return approximateFlexibleTypes(type, true);
        }
    };

    public static final Function1<JetType, JetType> APPROXIMATE_FLEXIBLE_TYPES_IN_ARGUMENTS = new Function1<JetType, JetType>() {
        @Override
        public JetType invoke(JetType type) {
            return approximateFlexibleTypes(type, false);
        }
    };

    public static final DescriptorRenderer SOURCE_CODE = commonBuilder()
            .setShortNames(false)
            .setTypeNormalizer(APPROXIMATE_FLEXIBLE_TYPES)
            .build();

    public static final DescriptorRenderer SOURCE_CODE_FOR_TYPE_ARGUMENTS = commonBuilder()
            .setShortNames(false)
            .setTypeNormalizer(APPROXIMATE_FLEXIBLE_TYPES_IN_ARGUMENTS)
            .build();

    public static final DescriptorRenderer SOURCE_CODE_SHORT_NAMES_IN_TYPES = commonBuilder()
            .setShortNames(true)
            .setTypeNormalizer(APPROXIMATE_FLEXIBLE_TYPES)
            .build();

    private static DescriptorRendererBuilder commonBuilder() {
        return new DescriptorRendererBuilder()
                .setNormalizedVisibilities(true)
                .setWithDefinedIn(false)
                .setShowInternalKeyword(false)
                .setOverrideRenderingPolicy(DescriptorRenderer.OverrideRenderingPolicy.RENDER_OVERRIDE)
                .setUnitReturnType(false);
    }
    
    // TODO: Copied from TypeUtils.kt in Kotlin IDEA plugin
    private static JetType approximateFlexibleTypes(JetType jetType,  Boolean outermost) {
        if (TypesPackage.isFlexible(jetType)) {
            Flexibility flexible = TypesPackage.flexibility(jetType);
            ClassifierDescriptor declarationDescriptor = flexible.getLowerBound().getConstructor().getDeclarationDescriptor();
            
            ClassDescriptor lowerClass = declarationDescriptor instanceof ClassDescriptor ? (ClassDescriptor) declarationDescriptor : null;
            
            boolean isCollection = lowerClass != null && CollectionClassMapping.getInstance().isMutableCollection(lowerClass);
            
            // (Mutable)Collection<T>! -> MutableCollection<T>?
            // Foo<(Mutable)Collection<T>!>! -> Foo<Collection<T>>?
            // Foo! -> Foo?
            // Foo<Bar!>! -> Foo<Bar>?

            JetType approximation;
            if (isCollection) {
                JetType collectionType = isMarkedReadOnly(jetType) ? flexible.getUpperBound() : flexible.getLowerBound();
                
                approximation = TypeUtils.makeNullableAsSpecified(collectionType, outermost);
            } else {
                approximation = outermost ? flexible.getUpperBound() : flexible.getLowerBound();
            }
            
            JetType approximated = approximateFlexibleTypes(approximation, true);
            return isMarkedNotNull(jetType) ? TypeUtils.makeNotNullable(approximated) : approximated;
        }
        
        List<TypeProjection> arguments = KotlinPackage.map(jetType.getArguments(), new Function1<TypeProjection, TypeProjection>() {
            @Override
            public TypeProjection invoke(TypeProjection originalProjection) {
                return new TypeProjectionImpl(originalProjection.getProjectionKind(), approximateFlexibleTypes(originalProjection.getType(), false));
            }
        });
        
        return new JetTypeImpl(
                jetType.getAnnotations(),
                jetType.getConstructor(),
                jetType.isNullable(),
                arguments,
                ErrorUtils.createErrorScope("This type is not supposed to be used in member resolution", true)
        );
    }
    
    private static boolean isMarkedReadOnly(JetType type) {
        return type.getAnnotations().findAnnotation(JvmAnnotationNames.JETBRAINS_READONLY_ANNOTATION) != null;
    }
    
    private static boolean isMarkedNotNull(JetType type) {
        return type.getAnnotations().findAnnotation(JvmAnnotationNames.JETBRAINS_NOT_NULL_ANNOTATION) != null;
    }

}

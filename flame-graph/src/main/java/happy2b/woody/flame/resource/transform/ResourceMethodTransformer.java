package happy2b.woody.flame.resource.transform;

import happy2b.woody.flame.resource.ResourceMethod;
import happy2b.woody.flame.resource.ResourceMethodManager;
import happy2b.woody.flame.tool.AGCTPredicate;
import happy2b.woody.api.id.IdGenerator;
import happy2b.woody.api.id.ParametricIdGenerator;
import happy2b.woody.util.MethodUtil;
import org.objectweb.asm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;

import static happy2b.woody.flame.resource.transform.ResourceMethodAdvice.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/23
 */
public class ResourceMethodTransformer implements ClassFileTransformer {

    private static final Logger log = LoggerFactory.getLogger(ResourceMethodTransformer.class);

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (classBeingRedefined == null) {
            return null;
        }
        if (!AGCTPredicate.acceptTracing(loader, className)) {
            return null;
        }

        ClassReader reader = new ClassReader(classfileBuffer);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);

        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                ResourceMethod includeMethod = ResourceMethodManager.findProfilingIncludeMethod(classBeingRedefined.getName(), name, descriptor);
                return includeMethod == null ? mv : new TracingMethodVisitor(Opcodes.ASM9, mv, includeMethod);
            }
        }, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();

    }

    public static class TracingMethodVisitor extends MethodVisitor {

        private ResourceMethod includeMethod;

        private Label startLabel, endLabel;
        private Label tryStartLabel, tryEndLabel, catchLabel;

        private boolean isStatic;
        private int localVarIndex;

        public TracingMethodVisitor(int api, MethodVisitor methodVisitor, ResourceMethod includeMethod) {
            super(api, methodVisitor);
            this.includeMethod = includeMethod;
            Method method = includeMethod.getMethod();
            this.isStatic = Modifier.isStatic(method.getModifiers());

            int paramSlotCount = 0;
            for (Class<?> paramType : method.getParameterTypes()) {
                paramSlotCount += (paramType == long.class || paramType == double.class) ? 2 : 1;
            }
            this.localVarIndex = paramSlotCount + (isStatic ? 0 : 1);
        }

        @Override
        public void visitCode() {
            startLabel = new Label();
            endLabel = new Label();
            tryStartLabel = new Label();
            tryEndLabel = new Label();
            catchLabel = new Label();

            mv.visitLabel(startLabel);

            mv.visitLdcInsn(includeMethod.getResourceType());
            mv.visitLdcInsn(includeMethod.getResource());
            mv.visitLdcInsn(includeMethod.getMethodPath());

            int order = includeMethod.getIdGenerator().getOrder();
            mv.visitLdcInsn(order);
            IdGenerator idGenerator = ResourceMethodManager.ID_GENERATORS[order];
            if (idGenerator instanceof ParametricIdGenerator) {
                if (includeMethod.getMethod().getParameterTypes().length == 0) {
                    throw new IllegalStateException("method " + includeMethod.getMethodName() + " has no parameter");
                }
                int paramIndex = ((ParametricIdGenerator<?>) idGenerator).paramIndex();
                mv.visitVarInsn(ALOAD, isStatic ? paramIndex : paramIndex + 1);
                mv.visitMethodInsn(INVOKESTATIC, ADVICE_CLASS, START_TRACE_WITH_PARAM_METHOD.getName(), MethodUtil.getMethodDescriptor(START_TRACE_WITH_PARAM_METHOD), false);
            } else {
                mv.visitMethodInsn(INVOKESTATIC, ADVICE_CLASS, START_TRACE_METHOD.getName(), MethodUtil.getMethodDescriptor(START_TRACE_METHOD), false);
            }

            mv.visitVarInsn(ASTORE, localVarIndex);

            mv.visitLabel(tryStartLabel);

            super.visitCode();
        }

        @Override
        public void visitInsn(int opcode) {
            if ((opcode >= IRETURN && opcode <= RETURN) || opcode == ATHROW) {
                mv.visitVarInsn(ALOAD, localVarIndex);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, PROFILING_TRACE_CLASS, FINISH_TRACE_METHOD.getName(), MethodUtil.getMethodDescriptor(FINISH_TRACE_METHOD), false);
                mv.visitLabel(tryEndLabel);
            }
            super.visitInsn(opcode);
        }


        @Override
        public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
            if (index >= localVarIndex) {
                index += 1;
            }
            super.visitLocalVariable(name, descriptor, signature, start, end, index);
        }

        @Override
        public void visitIincInsn(int varIndex, int increment) {
            if (varIndex >= localVarIndex) {
                varIndex += 1;
            }
            super.visitIincInsn(varIndex, increment);
        }

        @Override
        public void visitVarInsn(int opcode, int varIndex) {
            if (varIndex >= localVarIndex) {
                varIndex += 1;
            }
            super.visitVarInsn(opcode, varIndex);
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            mv.visitTryCatchBlock(tryStartLabel, tryEndLabel, catchLabel, "java/lang/Throwable");

            mv.visitLabel(catchLabel);
            int exceptionIndex = localVarIndex + 1;
            mv.visitVarInsn(ASTORE, exceptionIndex);

            mv.visitVarInsn(ALOAD, localVarIndex);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, PROFILING_TRACE_CLASS, FINISH_TRACE_METHOD.getName(), MethodUtil.getMethodDescriptor(FINISH_TRACE_METHOD), false);

            mv.visitVarInsn(ALOAD, exceptionIndex);
            mv.visitInsn(ATHROW);

            mv.visitLabel(endLabel);
            mv.visitLocalVariable("oneTrace", PROFILING_TRACE_CLASS_DESC, null, startLabel, endLabel, localVarIndex);

            super.visitMaxs(Math.max(maxStack, localVarIndex + 2), maxLocals);
        }
    }
}

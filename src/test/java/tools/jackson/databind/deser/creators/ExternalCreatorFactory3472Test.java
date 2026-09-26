package tools.jackson.databind.deser.creators;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.core.type.TypeReference;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExternalCreatorFactory3472Test
    extends DatabindTestUtil
{
    @JsonDeserialize(creatorFactory = ImmutableValueFactory.class)
    static class ImmutableValue {
        private final int id;
        private final String name;

        private ImmutableValue(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    static class ImmutableValueFactory {
        @JsonCreator
        static ImmutableValue create(@JsonProperty("id") int id,
                @JsonProperty("name") String name) {
            return new ImmutableValue(id, name);
        }
    }

    interface Vehicle {
        String licensePlate();
    }

    static class DefaultVehicle implements Vehicle {
        private final String licensePlate;

        private DefaultVehicle(String licensePlate) {
            this.licensePlate = licensePlate;
        }

        @Override
        public String licensePlate() {
            return licensePlate;
        }
    }

    static class VehicleFactory {
        @JsonCreator
        static Vehicle create(@JsonProperty("licensePlate") String licensePlate) {
            return new DefaultVehicle(licensePlate);
        }
    }

    @JsonDeserialize(creatorFactory = VehicleFactory.class)
    abstract static class VehicleMixin { }

    @JsonDeserialize(creatorFactory = TextValueFactory.class)
    static class TextValue {
        private final String value;

        private TextValue(String value) {
            this.value = value;
        }
    }

    static class TextValueFactory {
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        static TextValue create(String value) {
            return new TextValue(value);
        }
    }

    static class MixedValue {
        private final int value;

        private MixedValue(int value) {
            this.value = value;
        }
    }

    static class MixedValueFactory {
        static MixedValue create(int value) {
            return new MixedValue(value);
        }
    }

    abstract static class MixedValueFactoryMixin {
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        static MixedValue create(int value) {
            return null;
        }
    }

    static class InvalidFactoryTarget { }

    static class InvalidFactory {
        @JsonCreator
        static String create(@JsonProperty("value") String value) {
            return value;
        }
    }

    @JsonDeserialize(creatorFactory = InvalidFactory.class)
    static class OverrideTarget {
        private final String value;

        private OverrideTarget(String value) {
            this.value = value;
        }
    }

    static class OverrideTargetFactory {
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        static OverrideTarget create(String value) {
            return new OverrideTarget(value);
        }
    }

    static class MissingCreatorTarget { }

    static class MissingCreatorFactory {
        static MissingCreatorTarget create() {
            return new MissingCreatorTarget();
        }
    }

    @JsonDeserialize(creatorFactory = BoxFactory.class)
    static class Box<T> {
        private final T value;

        private Box(T value) {
            this.value = value;
        }
    }

    static class BoxFactory {
        @JsonCreator
        static <T> Box<T> create(@JsonProperty("value") T value) {
            return new Box<>(value);
        }
    }

    static class Item {
        public int id;
    }

    static class SharedTargetA {
        private final int value;

        private SharedTargetA(int value) {
            this.value = value;
        }
    }

    static class SharedTargetB {
        private final String value;

        private SharedTargetB(String value) {
            this.value = value;
        }
    }

    static class SharedFactory {
        @JsonCreator
        static SharedTargetA createA(@JsonProperty("value") int value) {
            return new SharedTargetA(value);
        }

        @JsonCreator
        static SharedTargetB createB(@JsonProperty("value") String value) {
            return new SharedTargetB(value);
        }
    }

    interface Container<T> {
        T value();
    }

    static class ContainerImpl<T> implements Container<T> {
        private final T value;

        private ContainerImpl(T value) {
            this.value = value;
        }

        @Override
        public T value() {
            return value;
        }
    }

    static class ContainerFactory {
        @JsonCreator
        static <T> ContainerImpl<T> create(@JsonProperty("value") T value) {
            return new ContainerImpl<>(value);
        }
    }

    static class IncompatibleContainerFactory {
        @JsonCreator
        static ContainerImpl<Integer> create(@JsonProperty("value") Integer value) {
            return new ContainerImpl<>(value);
        }
    }

    @JsonDeserialize(creatorFactory = SelfFactoryTarget.class)
    static class SelfFactoryTarget {
        private final int value;

        private SelfFactoryTarget(int value) {
            this.value = value;
        }

        @JsonCreator
        static SelfFactoryTarget create(@JsonProperty("value") int value) {
            return new SelfFactoryTarget(value);
        }
    }

    public static class PublicValue {
        private final int value;

        private PublicValue(int value) {
            this.value = value;
        }
    }

    public static class PublicValueFactory {
        @JsonCreator
        public static PublicValue create(@JsonProperty("value") int value) {
            return new PublicValue(value);
        }
    }

    @JsonDeserialize(builder = BuiltValue.Builder.class)
    public static class BuiltValue {
        private final int value;

        private BuiltValue(int value) {
            this.value = value;
        }

        @JsonCreator
        public static Builder builder() {
            return new Builder();
        }

        @JsonPOJOBuilder(withPrefix = "with")
        @JsonDeserialize(creatorFactory = BuiltValue.class)
        public static class Builder {
            private int value;

            private Builder() { }

            public Builder withValue(int value) {
                this.value = value;
                return this;
            }

            public BuiltValue build() {
                return new BuiltValue(value);
            }
        }
    }

    @Test
    void usesPropertiesBasedExternalFactory() throws Exception {
        ImmutableValue value = newJsonMapper().readValue(
                "{\"id\":7,\"name\":\"test\"}", ImmutableValue.class);

        assertEquals(7, value.id);
        assertEquals("test", value.name);
    }

    @Test
    void supportsProgrammaticFactoryForInterface() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .withConfigOverride(Vehicle.class,
                        override -> override.setCreatorFactory(VehicleFactory.class))
                .build();

        Vehicle value = mapper.readValue(
                "{\"licensePlate\":\"12-AB-CD\"}", Vehicle.class);

        assertInstanceOf(DefaultVehicle.class, value);
        assertEquals("12-AB-CD", value.licensePlate());
    }

    @Test
    void supportsFactoryConfiguredOnTargetMixin() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .addMixIn(Vehicle.class, VehicleMixin.class)
                .build();

        Vehicle value = mapper.readValue(
                "{\"licensePlate\":\"34-CD-EF\"}", Vehicle.class);

        assertInstanceOf(DefaultVehicle.class, value);
        assertEquals("34-CD-EF", value.licensePlate());
    }

    @Test
    void usesDelegatingExternalFactory() throws Exception {
        TextValue value = newJsonMapper().readValue("\"wrapped\"", TextValue.class);

        assertEquals("wrapped", value.value);
    }

    @Test
    void supportsCreatorAnnotationFromFactoryMixin() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .addMixIn(MixedValueFactory.class, MixedValueFactoryMixin.class)
                .withConfigOverride(MixedValue.class,
                        override -> override.setCreatorFactory(MixedValueFactory.class))
                .build();

        MixedValue value = mapper.readValue("37", MixedValue.class);

        assertEquals(37, value.value);
    }

    @Test
    void rejectsFactoryWithIncompatibleReturnType() {
        ObjectMapper mapper = JsonMapper.builder()
                .withConfigOverride(InvalidFactoryTarget.class,
                        override -> override.setCreatorFactory(InvalidFactory.class))
                .build();

        InvalidDefinitionException e = assertThrows(InvalidDefinitionException.class,
                () -> mapper.readValue("{\"value\":\"x\"}", InvalidFactoryTarget.class));
        assertTrue(e.getMessage().contains("incompatible return types: [java.lang.String]"));
        assertTrue(e.getMessage().contains(InvalidFactoryTarget.class.getName()));
    }

    @Test
    void programmaticFactoryOverridesAnnotation() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .withConfigOverride(OverrideTarget.class,
                        override -> override.setCreatorFactory(OverrideTargetFactory.class))
                .build();

        OverrideTarget value = mapper.readValue("\"configured\"", OverrideTarget.class);

        assertEquals("configured", value.value);
    }

    @Test
    void rejectsFactoryWithoutEnabledCreatorMethods() {
        ObjectMapper mapper = JsonMapper.builder()
                .withConfigOverride(MissingCreatorTarget.class,
                        override -> override.setCreatorFactory(MissingCreatorFactory.class))
                .build();

        InvalidDefinitionException e = assertThrows(InvalidDefinitionException.class,
                () -> mapper.readValue("{}", MissingCreatorTarget.class));
        assertTrue(e.getMessage().contains("has no enabled static @JsonCreator methods"));
    }

    @Test
    void resolvesGenericFactoryParameterFromTargetType() throws Exception {
        Box<Item> value = newJsonMapper().readValue(
                "{\"value\":{\"id\":42}}", new TypeReference<Box<Item>>() { });

        assertInstanceOf(Item.class, value.value);
        assertEquals(42, value.value.id);
    }

    @Test
    void sharedFactoryIgnoresCreatorsForOtherTargetTypes() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .withConfigOverride(SharedTargetA.class,
                        override -> override.setCreatorFactory(SharedFactory.class))
                .withConfigOverride(SharedTargetB.class,
                        override -> override.setCreatorFactory(SharedFactory.class))
                .build();

        SharedTargetA a = mapper.readValue("{\"value\":12}", SharedTargetA.class);
        SharedTargetB b = mapper.readValue("{\"value\":\"text\"}", SharedTargetB.class);

        assertEquals(12, a.value);
        assertEquals("text", b.value);
    }

    @Test
    void resolvesGenericFactoryImplementationReturnFromTargetType() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .withConfigOverride(Container.class,
                        override -> override.setCreatorFactory(ContainerFactory.class))
                .build();

        Container<Item> value = mapper.readValue(
                "{\"value\":{\"id\":43}}", new TypeReference<Container<Item>>() { });

        assertInstanceOf(ContainerImpl.class, value);
        assertInstanceOf(Item.class, value.value());
        assertEquals(43, value.value().id);
    }

    @Test
    void usesPublicFactoryWithoutAccessOverride() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .disable(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS)
                .withConfigOverride(PublicValue.class,
                        override -> override.setCreatorFactory(PublicValueFactory.class))
                .build();

        PublicValue value = mapper.readValue("{\"value\":44}", PublicValue.class);

        assertEquals(44, value.value);
    }

    @Test
    void rejectsFactoryWithIncompatibleGenericReturnType() {
        ObjectMapper mapper = JsonMapper.builder()
                .withConfigOverride(Container.class,
                        override -> override.setCreatorFactory(IncompatibleContainerFactory.class))
                .build();

        InvalidDefinitionException e = assertThrows(InvalidDefinitionException.class,
                () -> mapper.readValue("{\"value\":\"wrong\"}",
                        new TypeReference<Container<String>>() { }));

        assertTrue(e.getMessage().contains("ContainerImpl<java.lang.Integer>"));
    }

    @Test
    void targetClassFactoryIsNotRegisteredTwice() throws Exception {
        SelfFactoryTarget value = newJsonMapper().readValue(
                "{\"value\":46}", SelfFactoryTarget.class);

        assertEquals(46, value.value);
    }

    @Test
    void usesExternalZeroArgumentFactoryToInstantiateBuilder() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .disable(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS)
                .build();

        BuiltValue value = mapper.readValue("{\"value\":45}", BuiltValue.class);

        assertEquals(45, value.value);
    }
}

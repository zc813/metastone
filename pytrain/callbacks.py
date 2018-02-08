from keras.callbacks import Callback
from keras.callbacks import TensorBoard
from keras import backend as K
from tensorflow.python.saved_model import constants
from tensorflow.python.training import saver as tf_saver
from tensorflow.python.lib.io import file_io
from tensorflow.core.protobuf import saver_pb2
from tensorflow.python.util import compat
from tensorflow.python.ops import variables
import tensorflow as tf
import os

class SaveOnTrainingEnd(Callback):
    def __init__(self, path):
        super(SaveOnTrainingEnd, self).__init__()
        self.save_path = path

    def on_train_end(self, logs=None):
        self.model.save_weights(self.save_path)

class ExportGraphOnTrainingEnd(Callback):
    def __init__(self, path):
        super(ExportGraphOnTrainingEnd, self).__init__()
        self.save_path = path
        self.version = self.getCurrentVersion() + 1

    def on_train_end(self, logs=None):
        sess = K.get_session()
        builder = tf.saved_model.builder.SavedModelBuilder(os.path.join(self.save_path,str(self.version)))
        x = tf.saved_model.utils.build_tensor_info(self.model.input)
        y = tf.saved_model.utils.build_tensor_info(self.model.output)
        signature = tf.saved_model.signature_def_utils.build_signature_def(
            inputs={'input':x},
            outputs={'output':y},
            method_name=tf.saved_model.signature_constants.PREDICT_METHOD_NAME,
        )
        # builder.add_meta_graph_and_variables(
        #     sess, ['model'],
        #     signature_def_map={'signature':signature},
        #     # legacy_init_op=tf.group(tf.tables_initializer(), name='legacy_init_op')
        # )
        self.add_meta_graph_and_variables(
            builder,
            sess, ['model'],
            signature_def_map={'signature':signature},
            # legacy_init_op=tf.group(tf.tables_initializer(), name='legacy_init_op')
        )
        builder.save()
        print("Exported as version %d. " % self.version)
        self.version += 1

    def getCurrentVersion(self):
        return max(int(s) if s.isdigit() else -1 for s in os.listdir(self.save_path))

    # overrides builder.add_meta_graph_and_variables
    @staticmethod
    def add_meta_graph_and_variables(builder,
                                     sess,
                                     tags,
                                     signature_def_map=None,
                                     assets_collection=None,
                                     legacy_init_op=None,
                                     clear_devices=False,
                                     main_op=None):
        if builder._has_saved_variables:
            raise AssertionError("Graph state including variables and assets has "
                                 "already been saved. Please invoke "
                                 "`add_meta_graph()` instead.")

        # Validate the signature def map to ensure all included TensorInfos are
        # properly populated.
        builder._validate_signature_def_map(signature_def_map)

        # Save asset files and write them to disk, if any.
        builder._save_and_write_assets(assets_collection)

        # Create the variables sub-directory, if it does not exist.
        variables_dir = os.path.join(
            compat.as_text(builder._export_dir),
            compat.as_text(constants.VARIABLES_DIRECTORY))
        if not file_io.file_exists(variables_dir):
            file_io.recursive_create_dir(variables_dir)

        variables_path = os.path.join(
            compat.as_text(variables_dir),
            compat.as_text(constants.VARIABLES_FILENAME))

        if main_op is None:
            # Add legacy init op to the SavedModel.
            builder._maybe_add_legacy_init_op(legacy_init_op)
        else:
            builder._add_main_op(main_op)

        # Initialize a saver to generate a sharded output for all saveables in the
        # current scope.
        saver = tf_saver.Saver(
            variables._all_saveable_objects(),  # pylint: disable=protected-access
            sharded=True,
            write_version=saver_pb2.SaverDef.V2,
            allow_empty=True)

        # Save the variables. Also, disable writing the checkpoint state proto. The
        # file is not used during SavedModel loading. In addition, since a
        # SavedModel can be copied or moved, this avoids the checkpoint state to
        # become outdated.
        saver.save(sess, variables_path, write_meta_graph=False, write_state=False)

        # Export the meta graph def.

        # The graph almost certainly previously contained at least one Saver, and
        # possibly several (e.g. one for loading a pretrained embedding, and another
        # for the model weights).  However, a *new* Saver was just created that
        # includes all of the variables.  Removing the preexisting ones was the
        # motivation for the clear_extraneous_savers option, but it turns out that
        # there are edge cases where that option breaks the graph.  Until that is
        # resolved, we just leave the option set to False for now.
        # TODO(soergel): Reinstate clear_extraneous_savers=True when possible.
        meta_graph_def = saver.export_meta_graph(clear_devices=clear_devices, clear_extraneous_savers=True)

        # Tag the meta graph def and add it to the SavedModel.
        builder._tag_and_add_meta_graph(meta_graph_def, tags, signature_def_map)

        # Mark this instance of SavedModel as having saved variables, such that
        # subsequent attempts to save variables will fail.
        builder._has_saved_variables = True
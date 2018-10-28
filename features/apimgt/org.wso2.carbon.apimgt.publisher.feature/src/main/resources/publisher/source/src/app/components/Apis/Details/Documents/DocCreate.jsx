import React from 'react';
import PropTypes from 'prop-types';
import { withStyles } from '@material-ui/core/styles';
import Paper from '@material-ui/core/Paper';
import Grid from '@material-ui/core/Grid';
import TextField from '@material-ui/core/TextField';
import Typography from '@material-ui/core/Typography';
import CircularProgress from '@material-ui/core/CircularProgress';
import Dropzone from 'react-dropzone';
import Radio from '@material-ui/core/Radio';
import RadioGroup from '@material-ui/core/RadioGroup';
import FormControl from '@material-ui/core/FormControl';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import Button from '@material-ui/core/Button';
import { FormattedMessage } from 'react-intl';
import Alert from 'AppComponents/Shared/Alert';
import Select from '@material-ui/core/Select';
import MenuItem from '@material-ui/core/MenuItem';

import API from 'AppData/api.js';

const styles = theme => ({
    root: {
        ...theme.mixins.gutters(),
        paddingTop: theme.spacing.unit * 2,
        paddingBottom: theme.spacing.unit * 2,
    },
});

/**
 * @inheritdoc
 * @class DocCreate
 * @extends {React.Component}
 */
class DocCreate extends React.Component {
    /**
     * Creates an instance of DocCreate.
     * @param {any} props @inheritDoc
     * @memberof DocCreate
     */
    constructor(props) {
        super(props);
        this.client = new API();
        this.apiUUID = this.props.api.id;
        this.state = {
            name: '',
            type: 'How To',
            otherTypeName: null,
            isOtherType: false,
            summary: '',
            uploadMethod: 'FILE',
            files: [],
            fileUrl: null,
            loading: false
        };
    }

    /**
     * Handle Documentation file ondrop action when user drag and drop file to dopzone, This is passed through props
     * props to child component
     * @param {Object} files File object passed from DropZone library
     * @memberof DocCreate 
     */
    onDrop = (files) => {
        this.setState({
            files
        });
    }

    /**
     * Update FileURL when input get changed
     * @param {React.SyntheticEvent} e Event triggered when URL input field changed
     * @memberof DocCreate
     */
    fileUrlChange = (e) => {
        this.setState({ fileUrl: e.target.value });
    }

    /**
     * Update documentation name when input get changed
     * @param {React.SyntheticEvent} e Event triggered when Name input field changed
     * @memberof DocCreate
     */
    handleInputNameChange = (e) => {
        this.setState({ name: e.target.value });
    }

    /**
     * Update documentation summary when input get changed
     * @param {React.SyntheticEvent} e Event trigggered when Summary input field changed
     */
    handleInputSummaryChange = (e) => {
        this.setState({ summary: e.target.value });
    }

    /**
     * Update documentation type when input get changed
     * @param {React.SyntheticEvent} e Even triggered when type input field changed 
     */
    handleTypeChange = (e) => {
        //get corresponding type value to set in request 
        const type = DocCreate.DOCTYPES[e.target.value];
        if (type === 'OTHER') {
            this.setState({ isOtherType: true });
        } else {
            this.setState({ isOtherType: false });
            this.setState({ otherTypeName: null });
        }
        //set upload method of attachment to URL by default when public forum and support forum types
        //get selected
        if (type === 'PUBLIC_FORUM' || type === 'SUPPORT_FORUM') {
            this.setState({ uploadMethod: 'URL' })
        }
        this.setState({ type: e.target.value });
    }

    /**
     * Update upload method when input get changed
     * @param {React.SyntheticEvent} e Even triggered when attachment type input field changed 
     * @param value The value of selected input
     */
    handleUploadMethodChange = (e, value) => {
        this.setState({ uploadMethod: value });
        if (value === 'FILE') {
            this.setState({ fileUrl: null });
        } else {
            this.setState({ files: [] });
        }
    };

    /**
     * Update other document type when input get changed
     * @param {React.SyntheticEvent} e Even triggered when other type name input field changed 
     */
    handleOtherTypeChange = (e) => {
        this.setState({ otherTypeName: e.target.value });
    }

    /**
    * Make a blob using the content from file uploaded or from the URL and the send it over REST API.
    * @param {React.SyntheticEvent} e Click event of the submit button
    */
    handleSubmit = (e) => {
        e.preventDefault();
        const { summary, name, type, uploadMethod, fileUrl, files, isOtherType, otherTypeName } = this.state;
        this.setState({ loading: true });
        const body = {
            visibility: 'API_LEVEL', sourceType: uploadMethod, summary: summary, name: name,
            type: DocCreate.DOCTYPES[type]
        };
        if (isOtherType) {
            body.otherTypeName = otherTypeName;
        }
        if (uploadMethod === 'URL') {
            body.content = fileUrl;
            this.client
                .addDocument(this.apiUUID, body)
                .then(this.createDocumentCallback);
            // .catch((errorResponse) => {
            //     Alert.error('Something went wrong while adding the API Documentation!');
            //     this.setState({ loading: false });
            //     const { response } = errorResponse;
            //     if (response.body) {
            //         const { code, description, message } = response.body;
            //         const messageTxt = 'Error[' + code + ']: ' + description + ' | ' + message + '.';
            //         Alert.error(messageTxt);
            //     }
            //     console.log(errorResponse);
            // });
        } else if (uploadMethod === 'FILE') {
            if (files.length === 0) {
                this.setState({ loading: false });
                Alert.error('Select a file to upload.');
                console.log('Select a file to upload.');
                return;
            }
            body.fileName = files[0].name;
            this.client
                .addDocument(this.apiUUID, body)
                .then(this.uploadDocument)
                .then(this.createDocumentCallback);
            // .catch((errorResponse) => {
            //     Alert.error('Something went wrong while adding the API Documentation!');
            //     this.setState({ loading: false });
            //     const { response } = errorResponse;
            //     if (response.body) {
            //         const { code, description, message } = response.body;
            //         const messageTxt = 'Error[' + code + ']: ' + description + ' | ' + message + '.';
            //         Alert.error(messageTxt);
            //     }
            //     console.log(errorResponse);
            // });
        }
    }

    createDocumentCallback = (response) => {
        Alert.success(`${response.body.name} API Documentation Added Successfully.`);
        const documentId = response.body.documentId;
        const redirectURL = '/apis/' + this.apiUUID + '/documents/' + documentId + '/details';
        // this.props.history.push(redirectURL);
        this.setState({ loading: false });
    };

    uploadDocument = (response) => {
        Alert.success(`${response.body.name} API Documentation Added Successfully.`);
        this.client
            .addFileToDocument(this.apiUUID, response.body.documentId, this.state.files[0])
            .then(Alert.success(`${response.body.name} API Documentation Content Uploaded Successfully.`));
        this.setState({ loading: false });
    }

    /**
     * @returns {React.Component} @inheritDoc
     * @memberOf DocCreate
     */
    render() {
        const {
            uploadMethod, files, fileUrl, loading, name, type, summary, isOtherType, otherTypeName
        } = this.state;
        const { classes } = this.props;
        return (
            <Grid container className={classes.root} spacing={0} justify='center'>
                <Grid item md={10}>
                    <Paper className={classes.root} elevation={1}>
                        <Typography variant="h5" component="h3">
                            Add Document From URL or File
                        </Typography>
                        <form onSubmit={this.handleSubmit} className='add-document-form'>
                            <Grid container spacing={0} direction='column' justify='flex-start' alignItems='stretch'>
                                <Grid item>
                                    <Grid container spacing={16} direction='row' alignItems='center'>
                                        <Grid item style={{ flexGrow: 1 }}>
                                            <Grid container direction='row' justify='space-between'>
                                                <Grid item>
                                                    <Typography variant='subtitle1'>Name</Typography>
                                                </Grid>
                                                <Grid item>
                                                    <Typography variant='subtitle1'>:</Typography>
                                                </Grid>
                                            </Grid>
                                        </Grid>
                                        <Grid item lg={10} md={10} sm={10} xs={10} alignContent='stretch' alignItems='stretch'>
                                            <TextField
                                                fullWidth
                                                id='api-doc-name'
                                                label={'API Document Name'}
                                                value={name}
                                                margin='normal'
                                                required
                                                type='text'
                                                onChange={this.handleInputNameChange}
                                                helperText='Name of the API Documentation'
                                            />
                                        </Grid>
                                    </Grid>
                                </Grid>
                                <Grid item>
                                    <Grid container spacing={16} direction='row' alignItems='center'>
                                        <Grid item style={{ flexGrow: 1 }}>
                                            <Grid container direction='row' justify='space-between'>
                                                <Grid item>
                                                    <Typography variant='subtitle1'>Type</Typography>
                                                </Grid>
                                                <Grid item>
                                                    <Typography variant='subtitle1'>:</Typography>
                                                </Grid>
                                            </Grid>
                                        </Grid>
                                        <Grid item lg={10} md={10} sm={10} xs={10} alignContent='stretch' alignItems='stretch'>
                                            <Select
                                                className={classes.docTypeSelect}
                                                margin='none'
                                                value={type}
                                                onChange={this.handleTypeChange}
                                                MenuProps={{
                                                    PaperProps: {
                                                        style: {
                                                            width: 200,
                                                        },
                                                    },
                                                }}
                                            >
                                                {Object.keys(DocCreate.DOCTYPES).map(tempKey => (
                                                    <MenuItem
                                                        key={DocCreate.DOCTYPES[tempKey]}
                                                        value={tempKey}
                                                    >
                                                        {tempKey}
                                                    </MenuItem>
                                                ))}
                                            </Select>
                                            {isOtherType && (
                                                <FormControl className='horizontal full-width' fullWidth>
                                                    <TextField
                                                        fullWidth
                                                        id='otherType'
                                                        label='Other Type Name'
                                                        type='text'
                                                        name='otherType'
                                                        required
                                                        margin='normal'
                                                        value={otherTypeName}
                                                        onChange={this.handleOtherTypeChange}
                                                    />
                                                </FormControl>
                                            )}
                                        </Grid>
                                    </Grid>
                                </Grid>
                                <Grid item>
                                    <Grid container spacing={16} direction='row' alignItems='center'>
                                        <Grid item style={{ flexGrow: 1 }}>
                                            <Grid container direction='row' justify='space-between'>
                                                <Grid item>
                                                    <Typography variant='subtitle1'>Summary</Typography>
                                                </Grid>
                                                <Grid item>
                                                    <Typography variant='subtitle1'>:</Typography>
                                                </Grid>
                                            </Grid>
                                        </Grid>
                                        <Grid item lg={10} md={10} sm={10} xs={10} alignContent='stretch' alignItems='stretch'>
                                            <TextField
                                                fullWidth
                                                id='api-doc-summary'
                                                label={'API Document Summary'}
                                                value={summary}
                                                margin='normal'
                                                required
                                                type='text'
                                                inputProps={{ maxLength: 120 }}
                                                onChange={this.handleInputSummaryChange}
                                                helperText='Short description about the API Documentation'
                                            />
                                        </Grid>
                                    </Grid>
                                </Grid>
                                <Grid item>
                                    <Grid container spacing={16} direction='row' alignItems='center'>
                                        <Grid item style={{ flexGrow: 1 }}>
                                            <Grid container direction='row' justify='space-between'>
                                                <Grid item>
                                                    <Typography variant='subtitle1'>Attach Document</Typography>
                                                </Grid>
                                                <Grid item>
                                                    <Typography variant='subtitle1'>:</Typography>
                                                </Grid>
                                            </Grid>
                                        </Grid>
                                        <Grid item lg={10} md={10} sm={10} xs={10} container spacing={0} direction='column' justify='flex-start' alignItems='stretch'>
                                            <RadioGroup
                                                aria-label='inputType'
                                                name='inputType'
                                                value={uploadMethod}
                                                onChange={this.handleUploadMethodChange}
                                                className='horizontal'
                                                style={{ display: 'flex', flexDirection: 'row' }}
                                            >
                                                <FormControlLabel
                                                    value='FILE'
                                                    control={<Radio />}
                                                    label={<FormattedMessage id='file' defaultMessage='File' />}
                                                />
                                                <FormControlLabel
                                                    value='URL'
                                                    control={<Radio />}
                                                    label={<FormattedMessage id='url' defaultMessage='URL' />}
                                                />
                                            </RadioGroup>
                                            <Grid>
                                                {uploadMethod === 'FILE' && (
                                                    <FormControl className='horizontal dropzone-wrapper' fullWidth>
                                                        <div className='dropzone'>
                                                            <Dropzone onDrop={this.onDrop} multiple={false}>
                                                                <p><FormattedMessage
                                                                    id='try.dropping.some.files.here.or.click.to.select.files.to.upload'
                                                                    defaultMessage={'Try dropping some files here, or click to select files to upload.'}
                                                                />
                                                                </p>
                                                            </Dropzone>
                                                        </div>
                                                        <aside>
                                                            <h4><FormattedMessage id='uploaded.files' defaultMessage='Uploaded files' />
                                                            </h4>
                                                            <ul>
                                                                {files.map(f => (
                                                                    <li key={f.name}>
                                                                        {f.name} - {f.size} bytes
                                                    </li>
                                                                ))}
                                                            </ul>
                                                        </aside>
                                                    </FormControl>
                                                )}
                                                {uploadMethod === 'URL' && (
                                                    <FormControl className='horizontal full-width' fullWidth>
                                                        <TextField
                                                            fullWidth
                                                            id='fileUrl'
                                                            label='File URL'
                                                            type='text'
                                                            name='fileUrl'
                                                            required
                                                            margin='normal'
                                                            value={fileUrl}
                                                            onChange={this.fileUrlChange}
                                                        />
                                                    </FormControl>
                                                )}
                                            </Grid>
                                        </Grid>
                                    </Grid>
                                </Grid>
                            </Grid>
                            <Grid item>
                                <FormControl>
                                    <Grid container direction='row' alignItems='flex-start' spacing={16}>
                                        <Grid item>
                                            <Button
                                                variant='contained'
                                                disabled={loading}
                                                color='primary'
                                                type='submit'
                                            >
                                                Save
                                            </Button>
                                            {loading && (
                                                <CircularProgress size={24} className={classes.buttonProgress} />
                                            )}

                                        </Grid>
                                        <Grid item>
                                            <Button variant='outlined'>
                                                Cancel
                                            </Button>
                                        </Grid>
                                    </Grid>
                                </FormControl>
                            </Grid>
                        </form>
                    </Paper>
                </Grid>
            </Grid>
        );
    }

}

DocCreate.DOCTYPES = {
    'How To': 'HOWTO',
    'Samples & SDK': 'SAMPLES',
    'Public Forum': 'PUBLIC_FORUM',
    'Support Forum': 'SUPPORT_FORUM',
    'Other (Specify)': 'OTHER'
}

DocCreate.propTypes = {
    // classes: PropTypes.object.isRequired,
    classes: PropTypes.shape({}).isRequired,
    history: PropTypes.shape({
        push: PropTypes.func,
    }).isRequired,
};

export default withStyles(styles)(DocCreate);
